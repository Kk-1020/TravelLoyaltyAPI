# Travel Loyalty Points Tracker API

A Spring Boot microservice that powers a travel loyalty rewards system: users earn points on
travel bookings (flights, hotels, cruises, car rentals, activities), redeem them for rewards,
and view their balance and full transaction history.

Built as a focused study of the kind of microservice that backs a travel-loyalty platform.

## Stack

- **Java 17** · **Spring Boot 3.2** · **Spring Data JPA** · **Bean Validation**
- **MySQL 8** in production, **H2** in tests
- **JUnit 5** · **Mockito** · **Spring MockMvc** · **JaCoCo** (85% line-coverage gate)
- **Docker** multi-stage build · **Docker Compose** (app + MySQL)

## Architecture

```
controller/   REST endpoints (UserController, BookingController, RedemptionController)
service/      Business logic, @Transactional boundaries
repository/   Spring Data JPA repositories
model/        JPA entities — User, Booking, PointTransaction
dto/          Request / response records (Java records)
exception/    Domain exceptions + @RestControllerAdvice global handler
```

### Data model — three tables, simple FKs

| table                | key columns                                                           |
| -------------------- | --------------------------------------------------------------------- |
| `users`              | `id`, `email` (unique), `points_balance`                              |
| `bookings`           | `id`, `user_id` → users, `booking_type`, `amount`, `points_earned`    |
| `point_transactions` | `id`, `user_id` → users, `type` (EARN/REDEEM), `points_delta`, `balance_after`, `booking_id?` |

`point_transactions` is an append-only ledger: every change to a user's balance writes a row
with the signed delta and the resulting balance. This makes the history endpoint a single
indexed query and gives you the ability to audit any balance state.

### Points-per-dollar by booking type

| Type        | Multiplier |
| ----------- | ---------- |
| CRUISE      | 12         |
| FLIGHT      | 10         |
| HOTEL       | 8          |
| ACTIVITY    | 6          |
| CAR_RENTAL  | 5          |

Points are computed as `floor(amount * multiplier)` using `BigDecimal` to avoid float drift.

## Endpoints

| Method | Path                       | Description                            | Status |
| ------ | -------------------------- | -------------------------------------- | ------ |
| POST   | `/users`                   | Register a user                        | 201    |
| GET    | `/users/{id}`              | Get user profile                       | 200    |
| GET    | `/users/{id}/balance`      | Current points balance                 | 200    |
| GET    | `/users/{id}/history`      | Full transaction history (newest first)| 200    |
| POST   | `/bookings`                | Log a booking; auto-awards points      | 201    |
| POST   | `/redeem`                  | Redeem points for a reward             | 201    |
| GET    | `/actuator/health`         | Liveness check                         | 200    |

### Error responses

| Status | Code                   | When                                    |
| ------ | ---------------------- | --------------------------------------- |
| 400    | `VALIDATION_FAILED`    | Bean Validation failure                 |
| 404    | `USER_NOT_FOUND`       | Unknown user id                         |
| 409    | `EMAIL_ALREADY_EXISTS` | Duplicate email on registration         |
| 422    | `INSUFFICIENT_POINTS`  | Redeeming more points than user has     |

## Run it

### With Docker Compose (one command)

```bash
docker compose up --build
```

This brings up MySQL 8 + the API. The app waits for MySQL's healthcheck, then starts on
`http://localhost:8080`.

### Locally (you provide MySQL)

```bash
# Start a local MySQL with database "loyalty"
./mvnw spring-boot:run
```

Override the connection with environment variables:

```bash
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/loyalty \
SPRING_DATASOURCE_USERNAME=loyalty \
SPRING_DATASOURCE_PASSWORD=loyalty \
./mvnw spring-boot:run
```

### Try it out

```bash
# 1. Register
curl -X POST http://localhost:8080/users \
  -H 'Content-Type: application/json' \
  -d '{"firstName":"Grace","lastName":"Hopper","email":"grace@example.com"}'
# -> 201 {"id":1,"pointsBalance":0,...}

# 2. Book a flight ($500 * 10 = 5000 points)
curl -X POST http://localhost:8080/bookings \
  -H 'Content-Type: application/json' \
  -d '{"userId":1,"bookingType":"FLIGHT","description":"ATL-LHR","amount":500.00}'

# 3. Check balance
curl http://localhost:8080/users/1/balance
# -> {"userId":1,"pointsBalance":5000}

# 4. Redeem 1500 points
curl -X POST http://localhost:8080/redeem \
  -H 'Content-Type: application/json' \
  -d '{"userId":1,"points":1500,"rewardDescription":"$25 gift card"}'

# 5. Full history
curl http://localhost:8080/users/1/history
```

## Tests

```bash
./mvnw test                # run unit + integration tests
./mvnw verify              # also enforces 85% line-coverage gate via JaCoCo
```

Coverage report lands in `target/site/jacoco/index.html`.

The suite has four layers:

- **Service unit tests** — Mockito-driven, cover the points-math, balance updates, and all
  exception paths in `UserService`, `BookingService`, `RedemptionService`.
- **Controller slice tests** — `@WebMvcTest` per controller, validates HTTP status codes,
  JSON shapes, validation failures, and the global exception handler mappings.
- **End-to-end integration test** — `@SpringBootTest` against H2 that walks the full
  lifecycle: register → book → balance → redeem → history.

The JUnit 5 test scaffolding was generated with Claude as a starting point and then refined
by hand — the assertion details, edge cases (FLOOR rounding on fractional cents, redeeming
the exact balance, validation boundary cases), and integration flow were authored
deliberately to exercise the contract, not just the happy path.

## Project layout

```
.
├── Dockerfile                      Multi-stage: maven build → JRE-alpine runtime
├── docker-compose.yml              app + MySQL with healthchecks
├── pom.xml                         Spring Boot parent, JaCoCo 85% gate
├── src/main/java/com/arrivia/loyalty/
│   ├── LoyaltyApplication.java
│   ├── controller/  service/  repository/  model/  dto/  exception/
└── src/test/java/com/arrivia/loyalty/
    ├── LoyaltyIntegrationTest.java
    ├── service/    *ServiceTest.java
    └── controller/ *ControllerTest.java
```

## Design choices worth calling out

- **Single point-ledger table.** Earning and redeeming both write to `point_transactions`
  with a signed `points_delta` and a snapshot `balance_after`. One query gets you the full
  history; one column gets you the running balance reconciled.
- **`@Transactional` at the service.** A booking is balance update + booking row +
  ledger row. Redemption is balance update + ledger row. Both run as one DB transaction,
  so a partial failure can't leave the balance and the history disagreeing.
- **Records for DTOs.** Java records keep request/response payloads boilerplate-free and
  immutable; Lombok stays on the JPA entities where setters are needed for Hibernate.
- **`open-in-view: false`.** Forces the service layer to fully resolve what the controller
  needs — no surprise lazy loads at JSON-serialization time.
- **Non-root container user.** The Dockerfile runs the JVM as a `spring` user inside an
  Alpine JRE image.

## License

MIT

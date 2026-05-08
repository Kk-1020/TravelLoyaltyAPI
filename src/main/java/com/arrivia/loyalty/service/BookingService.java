package com.arrivia.loyalty.service;

import com.arrivia.loyalty.dto.BookingRequest;
import com.arrivia.loyalty.dto.BookingResponse;
import com.arrivia.loyalty.exception.UserNotFoundException;
import com.arrivia.loyalty.model.Booking;
import com.arrivia.loyalty.model.PointTransaction;
import com.arrivia.loyalty.model.User;
import com.arrivia.loyalty.repository.BookingRepository;
import com.arrivia.loyalty.repository.PointTransactionRepository;
import com.arrivia.loyalty.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class BookingService {

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final PointTransactionRepository pointTransactionRepository;

    public BookingService(UserRepository userRepository,
                          BookingRepository bookingRepository,
                          PointTransactionRepository pointTransactionRepository) {
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
        this.pointTransactionRepository = pointTransactionRepository;
    }

    /**
     * Records a booking, calculates points earned (multiplier per booking type),
     * updates the user's balance, and writes a corresponding EARN transaction.
     * The whole flow is one DB transaction.
     */
    @Transactional
    public BookingResponse createBooking(BookingRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new UserNotFoundException(request.userId()));

        int pointsEarned = calculatePoints(request.amount(), request.bookingType().getPointsPerDollar());

        Booking booking = Booking.builder()
                .user(user)
                .bookingType(request.bookingType())
                .description(request.description())
                .amount(request.amount())
                .pointsEarned(pointsEarned)
                .build();
        booking = bookingRepository.save(booking);

        int newBalance = user.getPointsBalance() + pointsEarned;
        user.setPointsBalance(newBalance);
        userRepository.save(user);

        PointTransaction txn = PointTransaction.builder()
                .user(user)
                .type(PointTransaction.TransactionType.EARN)
                .pointsDelta(pointsEarned)
                .balanceAfter(newBalance)
                .description(String.format("%s: %s", request.bookingType(), request.description()))
                .bookingId(booking.getId())
                .build();
        pointTransactionRepository.save(txn);

        return BookingResponse.from(booking);
    }

    /**
     * Points = floor(amount * pointsPerDollar). BigDecimal math, then int conversion.
     */
    int calculatePoints(BigDecimal amount, int pointsPerDollar) {
        return amount.multiply(BigDecimal.valueOf(pointsPerDollar))
                .setScale(0, RoundingMode.FLOOR)
                .intValueExact();
    }
}

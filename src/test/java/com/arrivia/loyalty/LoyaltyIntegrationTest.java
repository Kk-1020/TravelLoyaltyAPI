package com.arrivia.loyalty;

import com.arrivia.loyalty.dto.BookingRequest;
import com.arrivia.loyalty.dto.RedemptionRequest;
import com.arrivia.loyalty.dto.UserRequest;
import com.arrivia.loyalty.model.Booking.BookingType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Full-stack happy path: register -> book (earn) -> check balance -> redeem -> history.
 * Runs against in-memory H2 (test profile).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LoyaltyIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void fullLoyaltyLifecycle() throws Exception {
        // 1. Register
        UserRequest userReq = new UserRequest("Grace", "Hopper", "grace+e2e@example.com");
        MvcResult userResult = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Long userId = objectMapper.readTree(userResult.getResponse().getContentAsString())
                .get("id").asLong();

        // 2. Initial balance is 0
        mockMvc.perform(get("/users/" + userId + "/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pointsBalance").value(0));

        // 3. Book a flight ($300 * 10 = 3000 points)
        BookingRequest flight = new BookingRequest(userId, BookingType.FLIGHT,
                "ATL->SFO", new BigDecimal("300.00"));
        mockMvc.perform(post("/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(flight)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pointsEarned").value(3000));

        // 4. Book a hotel ($150 * 8 = 1200 points)
        BookingRequest hotel = new BookingRequest(userId, BookingType.HOTEL,
                "Hilton SF", new BigDecimal("150.00"));
        mockMvc.perform(post("/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hotel)))
                .andExpect(status().isCreated());

        // 5. Balance is 3000 + 1200 = 4200
        mockMvc.perform(get("/users/" + userId + "/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pointsBalance").value(4200));

        // 6. Redeem 1000 points
        RedemptionRequest redeem = new RedemptionRequest(userId, 1000, "$25 gift card");
        mockMvc.perform(post("/redeem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(redeem)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.balanceAfter").value(3200));

        // 7. Try to redeem too many points -> 422
        RedemptionRequest greedy = new RedemptionRequest(userId, 999_999, "Mansion");
        mockMvc.perform(post("/redeem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(greedy)))
                .andExpect(status().isUnprocessableEntity());

        // 8. History has 3 entries (2 EARN + 1 REDEEM), newest first
        mockMvc.perform(get("/users/" + userId + "/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].type").value("REDEEM"))
                .andExpect(jsonPath("$[0].balanceAfter").value(3200));
    }
}

package com.arrivia.loyalty.controller;

import com.arrivia.loyalty.dto.BookingRequest;
import com.arrivia.loyalty.dto.BookingResponse;
import com.arrivia.loyalty.exception.UserNotFoundException;
import com.arrivia.loyalty.model.Booking.BookingType;
import com.arrivia.loyalty.service.BookingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookingController.class)
@Import(BookingControllerTest.MockedServices.class)
class BookingControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private BookingService bookingService;

    @TestConfiguration
    static class MockedServices {
        @Bean BookingService bookingService() { return mock(BookingService.class); }
    }

    @Test
    void create_returns201() throws Exception {
        BookingResponse resp = new BookingResponse(
                10L, 1L, BookingType.FLIGHT, "JFK->LAX",
                new BigDecimal("250.00"), 2500, Instant.now());
        when(bookingService.createBooking(any(BookingRequest.class))).thenReturn(resp);

        BookingRequest req = new BookingRequest(1L, BookingType.FLIGHT, "JFK->LAX",
                new BigDecimal("250.00"));

        mockMvc.perform(post("/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.pointsEarned").value(2500))
                .andExpect(jsonPath("$.bookingType").value("FLIGHT"));
    }

    @Test
    void create_returns400ForMissingFields() throws Exception {
        String body = """
            { "userId": null, "amount": null }
            """;
        mockMvc.perform(post("/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void create_returns400ForZeroAmount() throws Exception {
        BookingRequest req = new BookingRequest(1L, BookingType.FLIGHT, "x",
                new BigDecimal("0.00"));
        mockMvc.perform(post("/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_returns404WhenUserMissing() throws Exception {
        when(bookingService.createBooking(any())).thenThrow(new UserNotFoundException(99L));
        BookingRequest req = new BookingRequest(99L, BookingType.HOTEL, "x",
                new BigDecimal("100"));
        mockMvc.perform(post("/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }
}

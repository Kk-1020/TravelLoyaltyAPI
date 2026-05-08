package com.arrivia.loyalty.controller;

import com.arrivia.loyalty.dto.RedemptionRequest;
import com.arrivia.loyalty.dto.TransactionResponse;
import com.arrivia.loyalty.exception.InsufficientPointsException;
import com.arrivia.loyalty.exception.UserNotFoundException;
import com.arrivia.loyalty.model.PointTransaction.TransactionType;
import com.arrivia.loyalty.service.RedemptionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RedemptionController.class)
@Import(RedemptionControllerTest.MockedServices.class)
class RedemptionControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private RedemptionService redemptionService;

    @TestConfiguration
    static class MockedServices {
        @Bean RedemptionService redemptionService() { return mock(RedemptionService.class); }
    }

    @Test
    void redeem_returns201() throws Exception {
        TransactionResponse resp = new TransactionResponse(
                99L, TransactionType.REDEEM, -1500, 3500,
                "Redemption: Free hotel night", null, Instant.now());
        when(redemptionService.redeem(any(RedemptionRequest.class))).thenReturn(resp);

        RedemptionRequest req = new RedemptionRequest(1L, 1500, "Free hotel night");
        mockMvc.perform(post("/redeem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pointsDelta").value(-1500))
                .andExpect(jsonPath("$.balanceAfter").value(3500))
                .andExpect(jsonPath("$.type").value("REDEEM"));
    }

    @Test
    void redeem_returns422WhenInsufficientPoints() throws Exception {
        when(redemptionService.redeem(any()))
                .thenThrow(new InsufficientPointsException(500, 100));

        RedemptionRequest req = new RedemptionRequest(1L, 500, "x");
        mockMvc.perform(post("/redeem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("INSUFFICIENT_POINTS"));
    }

    @Test
    void redeem_returns404WhenUserMissing() throws Exception {
        when(redemptionService.redeem(any())).thenThrow(new UserNotFoundException(99L));
        RedemptionRequest req = new RedemptionRequest(99L, 100, "x");
        mockMvc.perform(post("/redeem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    @Test
    void redeem_returns400ForInvalidPoints() throws Exception {
        RedemptionRequest req = new RedemptionRequest(1L, 0, "x");
        mockMvc.perform(post("/redeem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void redeem_returns400ForBlankRewardDescription() throws Exception {
        RedemptionRequest req = new RedemptionRequest(1L, 100, "");
        mockMvc.perform(post("/redeem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }
}

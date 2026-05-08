package com.arrivia.loyalty.controller;

import com.arrivia.loyalty.dto.BalanceResponse;
import com.arrivia.loyalty.dto.TransactionResponse;
import com.arrivia.loyalty.dto.UserRequest;
import com.arrivia.loyalty.dto.UserResponse;
import com.arrivia.loyalty.exception.EmailAlreadyExistsException;
import com.arrivia.loyalty.exception.UserNotFoundException;
import com.arrivia.loyalty.model.PointTransaction.TransactionType;
import com.arrivia.loyalty.service.RedemptionService;
import com.arrivia.loyalty.service.UserService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import(UserControllerTest.MockedServices.class)
class UserControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserService userService;
    @Autowired private RedemptionService redemptionService;

    @TestConfiguration
    static class MockedServices {
        @Bean UserService userService() { return mock(UserService.class); }
        @Bean RedemptionService redemptionService() { return mock(RedemptionService.class); }
    }

    @Test
    void register_returns201WithLocation() throws Exception {
        UserResponse mock = new UserResponse(1L, "Ada", "Lovelace",
                "ada@example.com", 0, Instant.now());
        when(userService.register(any(UserRequest.class))).thenReturn(mock);

        UserRequest req = new UserRequest("Ada", "Lovelace", "ada@example.com");

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/users/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("ada@example.com"))
                .andExpect(jsonPath("$.pointsBalance").value(0));
    }

    @Test
    void register_returns400ForInvalidEmail() throws Exception {
        UserRequest req = new UserRequest("A", "B", "not-an-email");
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fields.email").exists());
    }

    @Test
    void register_returns400ForBlankFields() throws Exception {
        UserRequest req = new UserRequest("", "", "");
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_returns409WhenEmailExists() throws Exception {
        when(userService.register(any())).thenThrow(new EmailAlreadyExistsException("dup@x.com"));

        UserRequest req = new UserRequest("A", "B", "dup@x.com");
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("EMAIL_ALREADY_EXISTS"));
    }

    @Test
    void getUser_returns200() throws Exception {
        when(userService.getById(1L)).thenReturn(
                new UserResponse(1L, "Ada", "L", "a@b.c", 250, Instant.now()));

        mockMvc.perform(get("/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pointsBalance").value(250));
    }

    @Test
    void getUser_returns404WhenMissing() throws Exception {
        when(userService.getById(99L)).thenThrow(new UserNotFoundException(99L));
        mockMvc.perform(get("/users/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("USER_NOT_FOUND"));
    }

    @Test
    void getBalance_returns200() throws Exception {
        when(userService.getBalance(1L)).thenReturn(750);
        mockMvc.perform(get("/users/1/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.pointsBalance").value(750));
    }

    @Test
    void getHistory_returns200WithList() throws Exception {
        TransactionResponse t1 = new TransactionResponse(
                1L, TransactionType.EARN, 500, 500,
                "Flight", 10L, Instant.now());
        TransactionResponse t2 = new TransactionResponse(
                2L, TransactionType.REDEEM, -200, 300,
                "Gift card", null, Instant.now());

        when(redemptionService.getHistory(1L)).thenReturn(List.of(t2, t1));

        mockMvc.perform(get("/users/1/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].type").value("REDEEM"))
                .andExpect(jsonPath("$[1].type").value("EARN"));
    }

    @Test
    void getHistory_returns404WhenUserMissing() throws Exception {
        when(redemptionService.getHistory(404L)).thenThrow(new UserNotFoundException(404L));
        mockMvc.perform(get("/users/404/history"))
                .andExpect(status().isNotFound());
    }
}

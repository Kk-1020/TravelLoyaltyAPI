package com.arrivia.loyalty.controller;

import com.arrivia.loyalty.dto.BalanceResponse;
import com.arrivia.loyalty.dto.TransactionResponse;
import com.arrivia.loyalty.dto.UserRequest;
import com.arrivia.loyalty.dto.UserResponse;
import com.arrivia.loyalty.service.RedemptionService;
import com.arrivia.loyalty.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final RedemptionService redemptionService;

    public UserController(UserService userService, RedemptionService redemptionService) {
        this.userService = userService;
        this.redemptionService = redemptionService;
    }

    @PostMapping
    public ResponseEntity<UserResponse> register(@Valid @RequestBody UserRequest request) {
        UserResponse created = userService.register(request);
        return ResponseEntity.created(URI.create("/users/" + created.id()))
                .body(created);
    }

    @GetMapping("/{id}")
    public UserResponse getUser(@PathVariable Long id) {
        return userService.getById(id);
    }

    @GetMapping("/{id}/balance")
    public BalanceResponse getBalance(@PathVariable Long id) {
        return new BalanceResponse(id, userService.getBalance(id));
    }

    @GetMapping("/{id}/history")
    public List<TransactionResponse> getHistory(@PathVariable Long id) {
        return redemptionService.getHistory(id);
    }
}

package com.arrivia.loyalty.controller;

import com.arrivia.loyalty.dto.RedemptionRequest;
import com.arrivia.loyalty.dto.TransactionResponse;
import com.arrivia.loyalty.service.RedemptionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/redeem")
public class RedemptionController {

    private final RedemptionService redemptionService;

    public RedemptionController(RedemptionService redemptionService) {
        this.redemptionService = redemptionService;
    }

    @PostMapping
    public ResponseEntity<TransactionResponse> redeem(@Valid @RequestBody RedemptionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(redemptionService.redeem(request));
    }
}

package com.arrivia.loyalty.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RedemptionRequest(
        @NotNull(message = "userId is required")
        Long userId,

        @NotNull(message = "points is required")
        @Min(value = 1, message = "points must be at least 1")
        Integer points,

        @NotBlank(message = "rewardDescription is required")
        @Size(max = 300)
        String rewardDescription
) {}

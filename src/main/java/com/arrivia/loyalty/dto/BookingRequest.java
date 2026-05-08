package com.arrivia.loyalty.dto;

import com.arrivia.loyalty.model.Booking.BookingType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record BookingRequest(
        @NotNull(message = "userId is required")
        Long userId,

        @NotNull(message = "bookingType is required")
        BookingType bookingType,

        @NotBlank(message = "description is required")
        @Size(max = 200)
        String description,

        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be greater than zero")
        BigDecimal amount
) {}

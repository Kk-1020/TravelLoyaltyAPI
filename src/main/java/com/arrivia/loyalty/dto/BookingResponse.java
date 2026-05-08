package com.arrivia.loyalty.dto;

import com.arrivia.loyalty.model.Booking;
import com.arrivia.loyalty.model.Booking.BookingType;

import java.math.BigDecimal;
import java.time.Instant;

public record BookingResponse(
        Long id,
        Long userId,
        BookingType bookingType,
        String description,
        BigDecimal amount,
        Integer pointsEarned,
        Instant createdAt
) {
    public static BookingResponse from(Booking b) {
        return new BookingResponse(
                b.getId(),
                b.getUser().getId(),
                b.getBookingType(),
                b.getDescription(),
                b.getAmount(),
                b.getPointsEarned(),
                b.getCreatedAt()
        );
    }
}

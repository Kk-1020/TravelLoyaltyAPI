package com.arrivia.loyalty.dto;

import com.arrivia.loyalty.model.PointTransaction;
import com.arrivia.loyalty.model.PointTransaction.TransactionType;

import java.time.Instant;

public record TransactionResponse(
        Long id,
        TransactionType type,
        Integer pointsDelta,
        Integer balanceAfter,
        String description,
        Long bookingId,
        Instant createdAt
) {
    public static TransactionResponse from(PointTransaction t) {
        return new TransactionResponse(
                t.getId(),
                t.getType(),
                t.getPointsDelta(),
                t.getBalanceAfter(),
                t.getDescription(),
                t.getBookingId(),
                t.getCreatedAt()
        );
    }
}

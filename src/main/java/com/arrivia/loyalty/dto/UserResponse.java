package com.arrivia.loyalty.dto;

import com.arrivia.loyalty.model.User;

import java.time.Instant;

public record UserResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        Integer pointsBalance,
        Instant createdAt
) {
    public static UserResponse from(User u) {
        return new UserResponse(
                u.getId(),
                u.getFirstName(),
                u.getLastName(),
                u.getEmail(),
                u.getPointsBalance(),
                u.getCreatedAt()
        );
    }
}

package com.arrivia.loyalty.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "point_transactions", indexes = {
        @Index(name = "idx_pt_user_created", columnList = "user_id, createdAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;

    /**
     * Positive for EARN, negative for REDEEM. Always reflects the signed delta
     * applied to the user's balance.
     */
    @Column(nullable = false)
    private Integer pointsDelta;

    @Column(nullable = false)
    private Integer balanceAfter;

    @Column(length = 300)
    private String description;

    /**
     * Optional foreign key to the originating booking (for EARN). Null for REDEEM.
     */
    @Column(name = "booking_id")
    private Long bookingId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public enum TransactionType {
        EARN,
        REDEEM
    }
}

package com.arrivia.loyalty.service;

import com.arrivia.loyalty.dto.RedemptionRequest;
import com.arrivia.loyalty.dto.TransactionResponse;
import com.arrivia.loyalty.exception.InsufficientPointsException;
import com.arrivia.loyalty.exception.UserNotFoundException;
import com.arrivia.loyalty.model.PointTransaction;
import com.arrivia.loyalty.model.User;
import com.arrivia.loyalty.repository.PointTransactionRepository;
import com.arrivia.loyalty.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedemptionServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PointTransactionRepository pointTransactionRepository;

    @InjectMocks
    private RedemptionService redemptionService;

    @Test
    @DisplayName("redeem: debits points and writes a REDEEM transaction")
    void redeem_debitsAndRecords() {
        User user = User.builder().id(1L).pointsBalance(5000).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(pointTransactionRepository.save(any(PointTransaction.class)))
                .thenAnswer(inv -> {
                    PointTransaction t = inv.getArgument(0);
                    t.setId(99L);
                    return t;
                });

        RedemptionRequest req = new RedemptionRequest(1L, 1500, "Free hotel night");
        TransactionResponse resp = redemptionService.redeem(req);

        assertThat(user.getPointsBalance()).isEqualTo(3500);
        assertThat(resp.pointsDelta()).isEqualTo(-1500);
        assertThat(resp.balanceAfter()).isEqualTo(3500);
        assertThat(resp.type()).isEqualTo(PointTransaction.TransactionType.REDEEM);
        assertThat(resp.description()).contains("Free hotel night");

        ArgumentCaptor<PointTransaction> captor = ArgumentCaptor.forClass(PointTransaction.class);
        verify(pointTransactionRepository).save(captor.capture());
        assertThat(captor.getValue().getBookingId()).isNull();
    }

    @Test
    @DisplayName("redeem: throws InsufficientPointsException when balance is too low")
    void redeem_throwsWhenBalanceTooLow() {
        User user = User.builder().id(1L).pointsBalance(100).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        RedemptionRequest req = new RedemptionRequest(1L, 500, "Anything");

        assertThatThrownBy(() -> redemptionService.redeem(req))
                .isInstanceOf(InsufficientPointsException.class)
                .hasMessageContaining("500")
                .hasMessageContaining("100");

        // Balance unchanged, no transaction recorded
        assertThat(user.getPointsBalance()).isEqualTo(100);
        verify(pointTransactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("redeem: redeeming exactly the available balance succeeds")
    void redeem_exactBalance() {
        User user = User.builder().id(1L).pointsBalance(1000).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(pointTransactionRepository.save(any(PointTransaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        TransactionResponse resp = redemptionService.redeem(
                new RedemptionRequest(1L, 1000, "Spa day"));

        assertThat(user.getPointsBalance()).isZero();
        assertThat(resp.balanceAfter()).isZero();
    }

    @Test
    @DisplayName("redeem: throws UserNotFoundException for unknown user")
    void redeem_throwsForUnknownUser() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> redemptionService.redeem(
                new RedemptionRequest(99L, 100, "x")))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("getHistory: returns all transactions for a user, mapped to DTOs")
    void getHistory_returnsAllTxnsForUser() {
        when(userRepository.existsById(1L)).thenReturn(true);

        PointTransaction t1 = PointTransaction.builder().id(1L)
                .user(User.builder().id(1L).build())
                .type(PointTransaction.TransactionType.EARN)
                .pointsDelta(500).balanceAfter(500)
                .description("Flight").bookingId(10L)
                .createdAt(Instant.now()).build();
        PointTransaction t2 = PointTransaction.builder().id(2L)
                .user(User.builder().id(1L).build())
                .type(PointTransaction.TransactionType.REDEEM)
                .pointsDelta(-200).balanceAfter(300)
                .description("Gift card")
                .createdAt(Instant.now()).build();

        when(pointTransactionRepository.findByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(t2, t1));

        List<TransactionResponse> history = redemptionService.getHistory(1L);

        assertThat(history).hasSize(2);
        assertThat(history.get(0).type()).isEqualTo(PointTransaction.TransactionType.REDEEM);
        assertThat(history.get(1).type()).isEqualTo(PointTransaction.TransactionType.EARN);
    }

    @Test
    @DisplayName("getHistory: throws UserNotFoundException when user doesn't exist")
    void getHistory_throwsForUnknownUser() {
        when(userRepository.existsById(404L)).thenReturn(false);
        assertThatThrownBy(() -> redemptionService.getHistory(404L))
                .isInstanceOf(UserNotFoundException.class);
        verify(pointTransactionRepository, never()).findByUserIdOrderByCreatedAtDesc(any());
    }
}

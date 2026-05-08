package com.arrivia.loyalty.service;

import com.arrivia.loyalty.dto.RedemptionRequest;
import com.arrivia.loyalty.dto.TransactionResponse;
import com.arrivia.loyalty.exception.InsufficientPointsException;
import com.arrivia.loyalty.exception.UserNotFoundException;
import com.arrivia.loyalty.model.PointTransaction;
import com.arrivia.loyalty.model.User;
import com.arrivia.loyalty.repository.PointTransactionRepository;
import com.arrivia.loyalty.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RedemptionService {

    private final UserRepository userRepository;
    private final PointTransactionRepository pointTransactionRepository;

    public RedemptionService(UserRepository userRepository,
                             PointTransactionRepository pointTransactionRepository) {
        this.userRepository = userRepository;
        this.pointTransactionRepository = pointTransactionRepository;
    }

    /**
     * Atomically debits points from the user balance and writes a REDEEM transaction.
     * Throws InsufficientPointsException when the requested points exceed available balance.
     */
    @Transactional
    public TransactionResponse redeem(RedemptionRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new UserNotFoundException(request.userId()));

        if (user.getPointsBalance() < request.points()) {
            throw new InsufficientPointsException(request.points(), user.getPointsBalance());
        }

        int newBalance = user.getPointsBalance() - request.points();
        user.setPointsBalance(newBalance);
        userRepository.save(user);

        PointTransaction txn = PointTransaction.builder()
                .user(user)
                .type(PointTransaction.TransactionType.REDEEM)
                .pointsDelta(-request.points())
                .balanceAfter(newBalance)
                .description("Redemption: " + request.rewardDescription())
                .bookingId(null)
                .build();
        return TransactionResponse.from(pointTransactionRepository.save(txn));
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getHistory(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }
        return pointTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(TransactionResponse::from)
                .toList();
    }
}

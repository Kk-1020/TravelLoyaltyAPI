package com.arrivia.loyalty.service;

import com.arrivia.loyalty.dto.UserRequest;
import com.arrivia.loyalty.dto.UserResponse;
import com.arrivia.loyalty.exception.EmailAlreadyExistsException;
import com.arrivia.loyalty.exception.UserNotFoundException;
import com.arrivia.loyalty.model.User;
import com.arrivia.loyalty.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public UserResponse register(UserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }
        User user = User.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .pointsBalance(0)
                .build();
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        return UserResponse.from(loadUser(id));
    }

    @Transactional(readOnly = true)
    public Integer getBalance(Long userId) {
        return loadUser(userId).getPointsBalance();
    }

    User loadUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }
}

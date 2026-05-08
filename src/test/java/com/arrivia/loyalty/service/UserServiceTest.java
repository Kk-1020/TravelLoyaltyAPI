package com.arrivia.loyalty.service;

import com.arrivia.loyalty.dto.UserRequest;
import com.arrivia.loyalty.dto.UserResponse;
import com.arrivia.loyalty.exception.EmailAlreadyExistsException;
import com.arrivia.loyalty.exception.UserNotFoundException;
import com.arrivia.loyalty.model.User;
import com.arrivia.loyalty.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private UserRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new UserRequest("Ada", "Lovelace", "ada@example.com");
    }

    @Test
    @DisplayName("register: persists a new user with zero balance")
    void register_persistsNewUser() {
        when(userRepository.existsByEmail("ada@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(42L);
            // simulate @PrePersist setting createdAt
            if (u.getCreatedAt() == null) u.setCreatedAt(Instant.now());
            return u;
        });

        UserResponse response = userService.register(validRequest);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();

        assertThat(saved.getEmail()).isEqualTo("ada@example.com");
        assertThat(saved.getPointsBalance()).isZero();
        assertThat(response.id()).isEqualTo(42L);
        assertThat(response.firstName()).isEqualTo("Ada");
    }

    @Test
    @DisplayName("register: throws EmailAlreadyExistsException for duplicate email")
    void register_throwsForDuplicateEmail() {
        when(userRepository.existsByEmail("ada@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(validRequest))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("ada@example.com");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("getById: returns user when found")
    void getById_returnsUser() {
        User user = User.builder().id(1L).firstName("Ada").lastName("L")
                .email("a@b.c").pointsBalance(150).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponse resp = userService.getById(1L);

        assertThat(resp.id()).isEqualTo(1L);
        assertThat(resp.pointsBalance()).isEqualTo(150);
    }

    @Test
    @DisplayName("getById: throws UserNotFoundException when missing")
    void getById_throwsWhenMissing() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.getById(99L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("getBalance: returns the user's points balance")
    void getBalance_returnsBalance() {
        User user = User.builder().id(1L).pointsBalance(500).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        assertThat(userService.getBalance(1L)).isEqualTo(500);
    }

    @Test
    @DisplayName("getBalance: throws when user not found")
    void getBalance_throwsWhenMissing() {
        when(userRepository.findById(7L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.getBalance(7L))
                .isInstanceOf(UserNotFoundException.class);
    }
}

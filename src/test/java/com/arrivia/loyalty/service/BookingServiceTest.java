package com.arrivia.loyalty.service;

import com.arrivia.loyalty.dto.BookingRequest;
import com.arrivia.loyalty.dto.BookingResponse;
import com.arrivia.loyalty.exception.UserNotFoundException;
import com.arrivia.loyalty.model.Booking;
import com.arrivia.loyalty.model.Booking.BookingType;
import com.arrivia.loyalty.model.PointTransaction;
import com.arrivia.loyalty.model.User;
import com.arrivia.loyalty.repository.BookingRepository;
import com.arrivia.loyalty.repository.PointTransactionRepository;
import com.arrivia.loyalty.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private PointTransactionRepository pointTransactionRepository;

    @InjectMocks
    private BookingService bookingService;

    @Test
    @DisplayName("createBooking: awards FLIGHT points (10 per dollar) and updates balance")
    void createBooking_awardsFlightPoints() {
        User user = User.builder().id(1L).firstName("A").lastName("B")
                .email("a@b.c").pointsBalance(100).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setId(10L);
            return b;
        });

        BookingRequest req = new BookingRequest(
                1L, BookingType.FLIGHT, "JFK->LAX", new BigDecimal("250.00")
        );

        BookingResponse resp = bookingService.createBooking(req);

        // 250 * 10 = 2500 points
        assertThat(resp.pointsEarned()).isEqualTo(2500);
        assertThat(user.getPointsBalance()).isEqualTo(100 + 2500);

        ArgumentCaptor<PointTransaction> txnCaptor = ArgumentCaptor.forClass(PointTransaction.class);
        verify(pointTransactionRepository).save(txnCaptor.capture());
        PointTransaction txn = txnCaptor.getValue();
        assertThat(txn.getType()).isEqualTo(PointTransaction.TransactionType.EARN);
        assertThat(txn.getPointsDelta()).isEqualTo(2500);
        assertThat(txn.getBalanceAfter()).isEqualTo(2600);
        assertThat(txn.getBookingId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("createBooking: HOTEL multiplier is 8")
    void createBooking_hotelMultiplier() {
        User user = User.builder().id(1L).pointsBalance(0).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingRequest req = new BookingRequest(
                1L, BookingType.HOTEL, "Marriott NYC", new BigDecimal("200.00")
        );
        BookingResponse resp = bookingService.createBooking(req);
        assertThat(resp.pointsEarned()).isEqualTo(1600); // 200*8
    }

    @Test
    @DisplayName("createBooking: CAR_RENTAL multiplier is 5")
    void createBooking_carRentalMultiplier() {
        User user = User.builder().id(1L).pointsBalance(0).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingRequest req = new BookingRequest(
                1L, BookingType.CAR_RENTAL, "Hertz 3 days", new BigDecimal("120.00")
        );
        assertThat(bookingService.createBooking(req).pointsEarned()).isEqualTo(600);
    }

    @Test
    @DisplayName("createBooking: CRUISE multiplier is 12")
    void createBooking_cruiseMultiplier() {
        User user = User.builder().id(1L).pointsBalance(0).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingRequest req = new BookingRequest(
                1L, BookingType.CRUISE, "Caribbean 7-day", new BigDecimal("1500.00")
        );
        assertThat(bookingService.createBooking(req).pointsEarned()).isEqualTo(18000);
    }

    @Test
    @DisplayName("createBooking: ACTIVITY multiplier is 6")
    void createBooking_activityMultiplier() {
        User user = User.builder().id(1L).pointsBalance(0).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingRequest req = new BookingRequest(
                1L, BookingType.ACTIVITY, "Snorkel tour", new BigDecimal("75.00")
        );
        assertThat(bookingService.createBooking(req).pointsEarned()).isEqualTo(450);
    }

    @Test
    @DisplayName("createBooking: throws UserNotFoundException for unknown user")
    void createBooking_throwsForUnknownUser() {
        when(userRepository.findById(404L)).thenReturn(Optional.empty());

        BookingRequest req = new BookingRequest(
                404L, BookingType.FLIGHT, "x", new BigDecimal("100.00")
        );

        assertThatThrownBy(() -> bookingService.createBooking(req))
                .isInstanceOf(UserNotFoundException.class);

        verify(bookingRepository, never()).save(any());
        verify(pointTransactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("calculatePoints: rounds DOWN fractional cents (FLOOR)")
    void calculatePoints_floorRounding() {
        // 99.99 * 10 = 999.9 -> floored to 999
        int points = bookingService.calculatePoints(new BigDecimal("99.99"), 10);
        assertThat(points).isEqualTo(999);
    }

    @Test
    @DisplayName("calculatePoints: zero amount yields zero points")
    void calculatePoints_zero() {
        assertThat(bookingService.calculatePoints(BigDecimal.ZERO, 10)).isZero();
    }
}

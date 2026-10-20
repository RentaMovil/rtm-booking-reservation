package com.rentamovil.booking.infrastructure.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.rentamovil.booking.domain.entity.AuthenticatedUser;
import com.rentamovil.booking.domain.entity.Rental;
import com.rentamovil.booking.domain.enums.RentalStatus;
import com.rentamovil.booking.domain.exception.ForbiddenException;
import com.rentamovil.booking.domain.port.in.RentalUseCases;
import com.rentamovil.booking.domain.port.in.ReservationUseCases;
import com.rentamovil.booking.infrastructure.web.dto.RentalResponse;

/** GET /rentals/admin: the full rental history, admins only. */
class AdminBookingControllerTest {

    private static final LocalDateTime PICKUP = LocalDateTime.of(2026, 11, 1, 10, 0);

    private final RentalUseCases rentals = mock(RentalUseCases.class);
    private final AdminBookingController controller =
            new AdminBookingController(mock(ReservationUseCases.class), rentals);

    private final AuthenticatedUser admin = new AuthenticatedUser(1, 1, "ADMIN", List.of());
    private final AuthenticatedUser client = new AuthenticatedUser(2, 2, "CLIENT", List.of());

    @Test
    @DisplayName("Returns the open rentals and also the closed ones")
    void includesTheClosedOnes() {
        Rental open = Rental.restore(2, 20, 301, 55, PICKUP.plusDays(3), null,
                new BigDecimal("1000.00"), null, RentalStatus.IN_PROGRESS);
        Rental closed = Rental.restore(1, 10, 302, 56, PICKUP, PICKUP.plusDays(2),
                new BigDecimal("500.00"), new BigDecimal("800.00"), RentalStatus.COMPLETED);
        when(rentals.findAll()).thenReturn(List.of(open, closed));

        List<RentalResponse> response = controller.listAllRentals(admin);

        assertThat(response).extracting(RentalResponse::getStatus)
                .containsExactly(RentalStatus.IN_PROGRESS, RentalStatus.COMPLETED);
        assertThat(response.get(1).getFinalMileage()).isEqualByComparingTo("800.00");
    }

    @Test
    @DisplayName("A client cannot see the rental history: 403")
    void clientCannot() {
        assertThatThrownBy(() -> controller.listAllRentals(client))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(rentals);
    }
}

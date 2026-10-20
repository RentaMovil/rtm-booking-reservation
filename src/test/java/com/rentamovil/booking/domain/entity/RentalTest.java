package com.rentamovil.booking.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.rentamovil.booking.domain.enums.RentalStatus;
import com.rentamovil.booking.domain.exception.BusinessRuleException;

class RentalTest {

    private static final LocalDateTime PICKUP = LocalDateTime.of(2026, 11, 1, 10, 0);
    private static final LocalDateTime RETURN = LocalDateTime.of(2026, 11, 5, 18, 30);

    private Rental inProgress() {
        return Rental.startPickup(1, 301, 55, PICKUP, new BigDecimal("15000.00"));
    }

    @Test
    @DisplayName("The pickup starts a rental IN_PROGRESS, with no end date or final mileage")
    void pickupStartsInProgress() {
        Rental rental = inProgress();

        assertThat(rental.getStatus()).isEqualTo(RentalStatus.IN_PROGRESS);
        assertThat(rental.getActualEndDate()).isNull();
        assertThat(rental.getFinalMileage()).isNull();
    }

    @Test
    @DisplayName("The return closes the rental with an end date and final mileage")
    void returnClosesTheRental() {
        Rental closed = inProgress().completeReturn(RETURN, new BigDecimal("15500.00"));

        assertThat(closed.getStatus()).isEqualTo(RentalStatus.COMPLETED);
        assertThat(closed.getActualEndDate()).isEqualTo(RETURN);
        assertThat(closed.getFinalMileage()).isEqualByComparingTo("15500.00");
    }

    @Test
    @DisplayName("The final mileage cannot be less than the initial one")
    void finalMileageCannotBeLower() {
        Rental rental = inProgress();

        assertThatThrownBy(() -> rental.completeReturn(RETURN, new BigDecimal("14000.00")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("menor");
    }

    @Test
    @DisplayName("The final mileage is required when returning")
    void finalMileageIsRequired() {
        assertThatThrownBy(() -> inProgress().completeReturn(RETURN, null))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Recording two returns changes nothing: it's idempotent")
    void idempotentReturn() {
        Rental closed = inProgress().completeReturn(RETURN, new BigDecimal("15500.00"));

        Rental again = closed.completeReturn(RETURN.plusHours(1), new BigDecimal("15600.00"));

        assertThat(again.getActualEndDate()).isEqualTo(RETURN);
        assertThat(again.getFinalMileage()).isEqualByComparingTo("15500.00");
    }

    @Test
    @DisplayName("The initial mileage is required at pickup")
    void initialMileageIsRequired() {
        assertThatThrownBy(() -> Rental.startPickup(1, 301, 55, PICKUP, null))
                .isInstanceOf(BusinessRuleException.class);
    }
}

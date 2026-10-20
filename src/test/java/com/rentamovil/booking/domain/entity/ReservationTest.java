package com.rentamovil.booking.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.rentamovil.booking.domain.enums.ReservationStatus;
import com.rentamovil.booking.domain.exception.BusinessRuleException;
import com.rentamovil.booking.domain.exception.InvalidDateRangeException;
import com.rentamovil.booking.domain.exception.InvalidStatusTransitionException;

/**
 * Domain tests: no Spring, no database (hexagonal-architecture.md, section 8).
 * Verify the invariants from 02-domain/entities-and-rules.md.
 *
 * <p>Some assertions check {@code hasMessageContaining(...)} against the production exception
 * message, which is in Spanish (production code wasn't translated, only this test file) — those
 * substrings are intentionally left in Spanish to match the real thrown message.
 */
class ReservationTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 11, 1, 10, 0);
    private static final LocalDateTime END = LocalDateTime.of(2026, 11, 5, 10, 0);

    private Reservation newReservation() {
        return Reservation.create(1, 301, null, 10, 11, START, END,
                new BigDecimal("120000.00"), BigDecimal.ZERO);
    }

    /** The reservation must be CONFIRMED to test most transitions. */
    private Reservation confirmed() {
        return newReservation()
                .transitionTo(ReservationStatus.PENDING_REVIEW)
                .transitionTo(ReservationStatus.CONFIRMED);
    }

    @Test
    @DisplayName("A new reservation is born PENDING_PAYMENT, never CONFIRMED")
    void newReservationStartsInPendingPayment() {
        assertThat(newReservation().getStatus()).isEqualTo(ReservationStatus.PENDING_PAYMENT);
    }

    @Test
    @DisplayName("INV-008: the total is always the sum of the two subtotals")
    void totalIsTheSumOfBothSubtotals() {
        Reservation withInsurance = Reservation.create(1, 301, 1, 10, 11, START, END,
                new BigDecimal("120000.00"), new BigDecimal("40000.00"));

        assertThat(withInsurance.getTotalAmount()).isEqualByComparingTo("160000.00");
    }

    @Test
    @DisplayName("Missing insurance is saved as 0, not as null")
    void missingInsuranceIsSavedAsZero() {
        Reservation withoutInsurance = Reservation.create(1, 301, null, 10, 11, START, END,
                new BigDecimal("120000.00"), null);

        assertThat(withoutInsurance.getInsuranceSubtotal()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("HU-BOOKING-003 scenario 2: the return must be after the pickup")
    void returnDateMustBeAfterPickup() {
        assertThatThrownBy(() -> Reservation.create(1, 301, null, 10, 11, START, START,
                BigDecimal.ZERO, BigDecimal.ZERO))
                .isInstanceOf(InvalidDateRangeException.class)
                .hasMessageContaining("posterior");
    }

    @Test
    @DisplayName("A return date before the pickup date is also rejected")
    void returnDateBeforePickupIsAlsoRejected() {
        assertThatThrownBy(() -> Reservation.create(1, 301, null, 10, 11, START,
                START.minusDays(1), BigDecimal.ZERO, BigDecimal.ZERO))
                .isInstanceOf(InvalidDateRangeException.class);
    }

    @Test
    @DisplayName("INV-010: a return branch different from the pickup branch is valid")
    void differentReturnBranchIsValid() {
        // pickup=10, return=99: no surcharge, no error.
        Reservation reservation = Reservation.create(1, 301, null, 10, 99, START, END,
                new BigDecimal("120000.00"), BigDecimal.ZERO);

        assertThat(reservation.getPickupBranchId()).isNotEqualTo(reservation.getReturnBranchId());
        assertThat(reservation.getTotalAmount()).isEqualByComparingTo("120000.00");
    }

    @Test
    @DisplayName("PENDING_PAYMENT can move to PENDING_REVIEW")
    void pendingPaymentMovesToPendingReview() {
        assertThat(newReservation().transitionTo(ReservationStatus.PENDING_REVIEW).getStatus())
                .isEqualTo(ReservationStatus.PENDING_REVIEW);
    }

    @Test
    @DisplayName("INV-009: a reservation is not CONFIRMED without going through PENDING_REVIEW")
    void cannotBeConfirmedWithoutReview() {
        assertThatThrownBy(() -> newReservation().transitionTo(ReservationStatus.CONFIRMED))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    @DisplayName("CANCELLED and COMPLETED are terminal states")
    void terminalStates() {
        Reservation cancelled = newReservation().transitionTo(ReservationStatus.CANCELLED);

        assertThatThrownBy(() -> cancelled.transitionTo(ReservationStatus.PENDING_PAYMENT))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    @DisplayName("Applying the same transition twice does not fail: it's idempotent")
    void idempotentTransition() {
        Reservation underReview = newReservation().transitionTo(ReservationStatus.PENDING_REVIEW);

        // at-least-once: the same PaymentReceiptUploaded event can arrive twice.
        Reservation again = underReview.transitionTo(ReservationStatus.PENDING_REVIEW);

        assertThat(again.getStatus()).isEqualTo(ReservationStatus.PENDING_REVIEW);
    }

    @Test
    @DisplayName("INV-016: a rejection with RETRY_ALLOWED sends the reservation back to PENDING_PAYMENT")
    void retryAllowedReturnsToPendingPayment() {
        Reservation underReview = newReservation().transitionTo(ReservationStatus.PENDING_REVIEW);

        assertThat(underReview.transitionTo(ReservationStatus.PENDING_PAYMENT).getStatus())
                .isEqualTo(ReservationStatus.PENDING_PAYMENT);
    }

    @Test
    @DisplayName("Changing the return branch doesn't touch the dates or the price")
    void changingTheBranchDoesNotTouchDatesOrPrice() {
        Reservation reservation = confirmed();

        Reservation changed = reservation.changeReturnBranch(99, END.minusDays(10));

        assertThat(changed.getStartDate()).isEqualTo(START);
        assertThat(changed.getEndDate()).isEqualTo(END);
        assertThat(changed.getTotalAmount()).isEqualByComparingTo(reservation.getTotalAmount());
    }

    @Test
    @DisplayName("A cancelled or completed reservation no longer changes branch")
    void aClosedReservationCannotChangeBranch() {
        Reservation cancelled = newReservation().transitionTo(ReservationStatus.CANCELLED);
        Reservation completed = confirmed().transitionTo(ReservationStatus.COMPLETED);

        assertThatThrownBy(() -> cancelled.changeReturnBranch(99, END.minusDays(10)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("cancelada");
        assertThatThrownBy(() -> completed.changeReturnBranch(99, END.minusDays(10)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("completada");
    }

    @Test
    @DisplayName("INV-013: the return branch can be changed up to 3 days before end_date")
    void branchCanBeChangedWithinTheDeadline() {
        Reservation reservation = confirmed();
        LocalDateTime withTimeToSpare = END.minusDays(10);

        assertThat(reservation.changeReturnBranch(99, withTimeToSpare).getReturnBranchId()).isEqualTo(99);
    }

    @Test
    @DisplayName("INV-013: past the deadline the return branch can no longer be changed")
    void branchCannotBeChangedPastTheDeadline() {
        Reservation reservation = confirmed();
        LocalDateTime pastTheDeadline = END.minusDays(2);

        assertThatThrownBy(() -> reservation.changeReturnBranch(99, pastTheDeadline))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("3 días");
    }

    @Test
    @DisplayName("A negative subtotal is rejected")
    void negativeSubtotalIsRejected() {
        assertThatThrownBy(() -> Reservation.create(1, 301, null, 10, 11, START, END,
                new BigDecimal("-1.00"), BigDecimal.ZERO))
                .isInstanceOf(BusinessRuleException.class);
    }
}

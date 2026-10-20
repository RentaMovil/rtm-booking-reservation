package com.rentamovil.booking.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.rentamovil.booking.domain.entity.Notification;
import com.rentamovil.booking.domain.entity.Reservation;
import com.rentamovil.booking.domain.enums.NotificationType;
import com.rentamovil.booking.domain.enums.ReservationStatus;
import com.rentamovil.booking.domain.exception.BusinessRuleException;
import com.rentamovil.booking.domain.exception.InvalidStatusTransitionException;
import com.rentamovil.booking.support.InMemoryNotificationRepository;
import com.rentamovil.booking.support.InMemoryReservationRepository;

/**
 * Reactions to payment and fleet events: transitions, notifications and idempotency.
 *
 * <p>A few assertions check {@code getMessage()}/{@code contains(...)} against the notification
 * text the production code builds, which is in Spanish (production code wasn't translated, only
 * this test file) — those substrings are intentionally left in Spanish to match the real text.
 */
class PaymentEventUseCaseImplTest {

    private static final Integer RESERVATION_ID = 12;
    private static final Integer CLIENT_ID = 7;
    private static final Integer PAYMENT_ID = 40;
    private static final LocalDateTime START = LocalDateTime.now().plusDays(10);

    private final InMemoryReservationRepository reservations = new InMemoryReservationRepository();
    private final InMemoryNotificationRepository notifications = new InMemoryNotificationRepository();
    private final PaymentEventUseCaseImpl useCase = new PaymentEventUseCaseImpl(reservations, notifications);

    private void givenReservation(ReservationStatus status) {
        reservations.put(Reservation.restore(RESERVATION_ID, CLIENT_ID, 301, null, 1, 2,
                LocalDateTime.now().minusHours(2), START, START.plusDays(4),
                new BigDecimal("400000.00"), BigDecimal.ZERO, status));
    }

    private ReservationStatus currentStatus() {
        return reservations.findById(RESERVATION_ID).orElseThrow().getStatus();
    }

    // --- Task 2: receipt uploaded ---

    @Test
    @DisplayName("Receipt uploaded: PENDING_PAYMENT moves to PENDING_REVIEW, no notification")
    void uploadedReceiptMovesToReview() {
        givenReservation(ReservationStatus.PENDING_PAYMENT);

        assertThat(useCase.onPaymentSubmitted(RESERVATION_ID, PAYMENT_ID)).isTrue();

        assertThat(currentStatus()).isEqualTo(ReservationStatus.PENDING_REVIEW);
        assertThat(notifications.all()).isEmpty();
    }

    @Test
    @DisplayName("Repeated receipt upload: ignored, writes nothing")
    void repeatedReceiptIsIgnored() {
        givenReservation(ReservationStatus.PENDING_REVIEW);

        assertThat(useCase.onPaymentSubmitted(RESERVATION_ID, PAYMENT_ID)).isFalse();

        assertThat(reservations.saveCount()).isZero();
    }

    @Test
    @DisplayName("Receipt over a cancelled reservation: invalid transition")
    void receiptOverCancelledFails() {
        givenReservation(ReservationStatus.CANCELLED);

        assertThatThrownBy(() -> useCase.onPaymentSubmitted(RESERVATION_ID, PAYMENT_ID))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    @DisplayName("Payment approved without a prior receipt: invalid transition (that's why PaymentSubmitted exists)")
    void approvedPaymentWithoutAReceiptFails() {
        givenReservation(ReservationStatus.PENDING_PAYMENT);

        assertThatThrownBy(() -> useCase.onPaymentCompleted(RESERVATION_ID, PAYMENT_ID, 3))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    @DisplayName("Full flow: receipt then approved payment leaves the reservation CONFIRMED with one notification")
    void fullFlowConfirmsTheReservation() {
        givenReservation(ReservationStatus.PENDING_PAYMENT);

        useCase.onPaymentSubmitted(RESERVATION_ID, PAYMENT_ID);
        assertThat(useCase.onPaymentCompleted(RESERVATION_ID, PAYMENT_ID, 3)).isTrue();

        assertThat(currentStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(notifications.all()).singleElement().satisfies(n -> {
            assertThat(n.getType()).isEqualTo(NotificationType.PAYMENT_APPROVED);
            assertThat(n.getPersonId()).isEqualTo(CLIENT_ID);
            assertThat(n.getPaymentId()).isEqualTo(PAYMENT_ID);
        });
    }

    // --- Task 4: repeated events don't duplicate notifications ---

    @Test
    @DisplayName("Repeated payment approved: the second time is ignored and doesn't notify again")
    void repeatedApprovedPaymentDoesNotDuplicate() {
        givenReservation(ReservationStatus.PENDING_REVIEW);

        assertThat(useCase.onPaymentCompleted(RESERVATION_ID, PAYMENT_ID, 3)).isTrue();
        assertThat(useCase.onPaymentCompleted(RESERVATION_ID, PAYMENT_ID, 3)).isFalse();

        assertThat(notifications.all()).hasSize(1);
    }

    @Test
    @DisplayName("A late approved payment over an already completed reservation: ignored without error")
    void lateApprovedPaymentIsIgnored() {
        givenReservation(ReservationStatus.COMPLETED);

        assertThat(useCase.onPaymentCompleted(RESERVATION_ID, PAYMENT_ID, 3)).isFalse();

        assertThat(currentStatus()).isEqualTo(ReservationStatus.COMPLETED);
        assertThat(notifications.all()).isEmpty();
    }

    @Test
    @DisplayName("Rejection with retry: goes back to PENDING_PAYMENT; repeated doesn't duplicate the notification")
    void rejectionWithRetry() {
        givenReservation(ReservationStatus.PENDING_REVIEW);

        assertThat(useCase.onPaymentRejected(RESERVATION_ID, PAYMENT_ID, "RETRY_ALLOWED", "Illegible")).isTrue();
        assertThat(useCase.onPaymentRejected(RESERVATION_ID, PAYMENT_ID, "RETRY_ALLOWED", "Illegible")).isFalse();

        assertThat(currentStatus()).isEqualTo(ReservationStatus.PENDING_PAYMENT);
        assertThat(notifications.all()).singleElement().satisfies(n -> {
            assertThat(n.getType()).isEqualTo(NotificationType.PAYMENT_REJECTED);
            assertThat(n.getMessage()).contains("Puedes subir otro comprobante").contains("Illegible");
        });
    }

    @Test
    @DisplayName("Rejection with cancellation: moves to CANCELLED; repeated doesn't duplicate the notification")
    void rejectionWithCancellation() {
        givenReservation(ReservationStatus.PENDING_REVIEW);

        assertThat(useCase.onPaymentRejected(RESERVATION_ID, PAYMENT_ID, "RESERVATION_CANCELLED", null)).isTrue();
        assertThat(useCase.onPaymentRejected(RESERVATION_ID, PAYMENT_ID, "RESERVATION_CANCELLED", null)).isFalse();

        assertThat(currentStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(notifications.all()).singleElement()
                .satisfies(n -> assertThat(n.getMessage()).contains("fue cancelada"));
    }

    @Test
    @DisplayName("Rejection with an unknown outcome: error, doesn't touch the reservation")
    void unknownOutcome() {
        givenReservation(ReservationStatus.PENDING_REVIEW);

        assertThatThrownBy(() -> useCase.onPaymentRejected(RESERVATION_ID, PAYMENT_ID, "MAYBE", null))
                .isInstanceOf(BusinessRuleException.class)
                .hasFieldOrPropertyWithValue("code", "UNKNOWN_PAYMENT_OUTCOME");

        assertThat(reservations.saveCount()).isZero();
        assertThat(currentStatus()).isEqualTo(ReservationStatus.PENDING_REVIEW);
    }

    @Test
    @DisplayName("Repeated invoice: notified only once per invoiceId")
    void repeatedInvoiceDoesNotDuplicate() {
        givenReservation(ReservationStatus.CONFIRMED);

        assertThat(useCase.onInvoiceGenerated(RESERVATION_ID, 90, PAYMENT_ID)).isTrue();
        assertThat(useCase.onInvoiceGenerated(RESERVATION_ID, 90, PAYMENT_ID)).isFalse();

        assertThat(notifications.all()).singleElement()
                .satisfies(n -> assertThat(n.getInvoiceId()).isEqualTo(90));
    }

    @Test
    @DisplayName("Repeated maintenance: one notification per person and maintenance")
    void repeatedMaintenanceDoesNotDuplicate() {
        assertThat(useCase.onMaintenanceCompleted(CLIENT_ID, 5, "Kia Picanto")).isTrue();
        assertThat(useCase.onMaintenanceCompleted(CLIENT_ID, 5, "Kia Picanto")).isFalse();
        // The same maintenance for a different person is notified.
        assertThat(useCase.onMaintenanceCompleted(99, 5, "Kia Picanto")).isTrue();

        assertThat(notifications.all()).extracting(Notification::getPersonId).containsExactly(CLIENT_ID, 99);
    }

    @Test
    @DisplayName("Event over a reservation that doesn't exist: RESERVATION_NOT_FOUND")
    void nonexistentReservation() {
        assertThatThrownBy(() -> useCase.onPaymentSubmitted(404, PAYMENT_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasFieldOrPropertyWithValue("code", "RESERVATION_NOT_FOUND");
    }
}

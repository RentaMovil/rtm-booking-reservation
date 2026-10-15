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

/** Reacciones a los eventos de pagos y flota: transiciones, notificaciones e idempotencia. */
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

    // --- Tarea 2: comprobante subido ---

    @Test
    @DisplayName("Comprobante subido: PENDING_PAYMENT pasa a PENDING_REVIEW, sin notificación")
    void comprobanteSubidoPasaARevision() {
        givenReservation(ReservationStatus.PENDING_PAYMENT);

        assertThat(useCase.onPaymentSubmitted(RESERVATION_ID, PAYMENT_ID)).isTrue();

        assertThat(currentStatus()).isEqualTo(ReservationStatus.PENDING_REVIEW);
        assertThat(notifications.all()).isEmpty();
    }

    @Test
    @DisplayName("Comprobante subido repetido: se ignora y no escribe nada")
    void comprobanteRepetidoSeIgnora() {
        givenReservation(ReservationStatus.PENDING_REVIEW);

        assertThat(useCase.onPaymentSubmitted(RESERVATION_ID, PAYMENT_ID)).isFalse();

        assertThat(reservations.saveCount()).isZero();
    }

    @Test
    @DisplayName("Comprobante sobre una reserva cancelada: transición inválida")
    void comprobanteSobreCanceladaFalla() {
        givenReservation(ReservationStatus.CANCELLED);

        assertThatThrownBy(() -> useCase.onPaymentSubmitted(RESERVATION_ID, PAYMENT_ID))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    @DisplayName("Pago aprobado sin comprobante previo: transición inválida (por eso existe PaymentSubmitted)")
    void pagoAprobadoSinComprobanteFalla() {
        givenReservation(ReservationStatus.PENDING_PAYMENT);

        assertThatThrownBy(() -> useCase.onPaymentCompleted(RESERVATION_ID, PAYMENT_ID, 3))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    @DisplayName("Flujo completo: comprobante y luego pago aprobado deja la reserva CONFIRMED con una notificación")
    void flujoCompletoConfirmaLaReserva() {
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

    // --- Tarea 4: eventos repetidos no duplican notificaciones ---

    @Test
    @DisplayName("Pago aprobado repetido: la segunda vez se ignora y no vuelve a notificar")
    void pagoAprobadoRepetidoNoDuplica() {
        givenReservation(ReservationStatus.PENDING_REVIEW);

        assertThat(useCase.onPaymentCompleted(RESERVATION_ID, PAYMENT_ID, 3)).isTrue();
        assertThat(useCase.onPaymentCompleted(RESERVATION_ID, PAYMENT_ID, 3)).isFalse();

        assertThat(notifications.all()).hasSize(1);
    }

    @Test
    @DisplayName("Pago aprobado atrasado sobre una reserva ya completada: se ignora sin error")
    void pagoAprobadoAtrasadoSeIgnora() {
        givenReservation(ReservationStatus.COMPLETED);

        assertThat(useCase.onPaymentCompleted(RESERVATION_ID, PAYMENT_ID, 3)).isFalse();

        assertThat(currentStatus()).isEqualTo(ReservationStatus.COMPLETED);
        assertThat(notifications.all()).isEmpty();
    }

    @Test
    @DisplayName("Rechazo con reintento: vuelve a PENDING_PAYMENT; repetido no duplica la notificación")
    void rechazoConReintento() {
        givenReservation(ReservationStatus.PENDING_REVIEW);

        assertThat(useCase.onPaymentRejected(RESERVATION_ID, PAYMENT_ID, "RETRY_ALLOWED", "Ilegible")).isTrue();
        assertThat(useCase.onPaymentRejected(RESERVATION_ID, PAYMENT_ID, "RETRY_ALLOWED", "Ilegible")).isFalse();

        assertThat(currentStatus()).isEqualTo(ReservationStatus.PENDING_PAYMENT);
        assertThat(notifications.all()).singleElement().satisfies(n -> {
            assertThat(n.getType()).isEqualTo(NotificationType.PAYMENT_REJECTED);
            assertThat(n.getMessage()).contains("Puedes subir otro comprobante").contains("Ilegible");
        });
    }

    @Test
    @DisplayName("Rechazo con cancelación: pasa a CANCELLED; repetido no duplica la notificación")
    void rechazoConCancelacion() {
        givenReservation(ReservationStatus.PENDING_REVIEW);

        assertThat(useCase.onPaymentRejected(RESERVATION_ID, PAYMENT_ID, "RESERVATION_CANCELLED", null)).isTrue();
        assertThat(useCase.onPaymentRejected(RESERVATION_ID, PAYMENT_ID, "RESERVATION_CANCELLED", null)).isFalse();

        assertThat(currentStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(notifications.all()).singleElement()
                .satisfies(n -> assertThat(n.getMessage()).contains("fue cancelada"));
    }

    @Test
    @DisplayName("Rechazo con un outcome desconocido: error y no toca la reserva")
    void outcomeDesconocido() {
        givenReservation(ReservationStatus.PENDING_REVIEW);

        assertThatThrownBy(() -> useCase.onPaymentRejected(RESERVATION_ID, PAYMENT_ID, "MAYBE", null))
                .isInstanceOf(BusinessRuleException.class)
                .hasFieldOrPropertyWithValue("code", "UNKNOWN_PAYMENT_OUTCOME");

        assertThat(reservations.saveCount()).isZero();
        assertThat(currentStatus()).isEqualTo(ReservationStatus.PENDING_REVIEW);
    }

    @Test
    @DisplayName("Factura repetida: se notifica una sola vez por invoiceId")
    void facturaRepetidaNoDuplica() {
        givenReservation(ReservationStatus.CONFIRMED);

        assertThat(useCase.onInvoiceGenerated(RESERVATION_ID, 90, PAYMENT_ID)).isTrue();
        assertThat(useCase.onInvoiceGenerated(RESERVATION_ID, 90, PAYMENT_ID)).isFalse();

        assertThat(notifications.all()).singleElement()
                .satisfies(n -> assertThat(n.getInvoiceId()).isEqualTo(90));
    }

    @Test
    @DisplayName("Mantenimiento repetido: una notificación por persona y mantenimiento")
    void mantenimientoRepetidoNoDuplica() {
        assertThat(useCase.onMaintenanceCompleted(CLIENT_ID, 5, "Kia Picanto")).isTrue();
        assertThat(useCase.onMaintenanceCompleted(CLIENT_ID, 5, "Kia Picanto")).isFalse();
        // El mismo mantenimiento para otra persona sí se notifica.
        assertThat(useCase.onMaintenanceCompleted(99, 5, "Kia Picanto")).isTrue();

        assertThat(notifications.all()).extracting(Notification::getPersonId).containsExactly(CLIENT_ID, 99);
    }

    @Test
    @DisplayName("Evento sobre una reserva que no existe: RESERVATION_NOT_FOUND")
    void reservaInexistente() {
        assertThatThrownBy(() -> useCase.onPaymentSubmitted(404, PAYMENT_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasFieldOrPropertyWithValue("code", "RESERVATION_NOT_FOUND");
    }
}

package com.rentamovil.booking.infrastructure.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.rentamovil.booking.domain.port.in.PaymentEventUseCases;
import com.rentamovil.booking.infrastructure.web.dto.InternalEventRequest;
import com.rentamovil.booking.infrastructure.web.dto.InternalEventResponse;
import com.rentamovil.booking.infrastructure.web.exception.InvalidEventException;

/** POST /internal/events: valida la estructura y delega en PaymentEventUseCases. */
class InternalEventControllerTest {

    private final PaymentEventUseCases useCases = mock(PaymentEventUseCases.class);
    private final InternalEventController controller = new InternalEventController(useCases);

    private InternalEventResponse send(String type, Map<String, Object> payload) {
        return controller.receive(new InternalEventRequest("evt-1", type, payload));
    }

    @Test
    @DisplayName("PaymentSubmitted delega en onPaymentSubmitted y responde PROCESSED")
    void paymentSubmittedDelega() {
        when(useCases.onPaymentSubmitted(12, 40)).thenReturn(true);

        InternalEventResponse response = send("PaymentSubmitted", Map.of("reservationId", 12, "paymentId", 40));

        assertThat(response.getResult()).isEqualTo(InternalEventResponse.PROCESSED);
        assertThat(response.getType()).isEqualTo("PaymentSubmitted");
        assertThat(response.getEventId()).isEqualTo("evt-1");
    }

    @Test
    @DisplayName("Un evento ya aplicado responde DUPLICATE_IGNORED (200, no es un error)")
    void eventoRepetido() {
        when(useCases.onPaymentCompleted(12, 40, 3)).thenReturn(false);

        InternalEventResponse response = send("PaymentCompleted",
                Map.of("reservationId", 12, "paymentId", 40, "actorUserId", 3));

        assertThat(response.getResult()).isEqualTo(InternalEventResponse.DUPLICATE_IGNORED);
    }

    @Test
    @DisplayName("actorUserId es opcional en PaymentCompleted")
    void actorOpcional() {
        send("PaymentCompleted", Map.of("reservationId", 12, "paymentId", 40));

        verify(useCases).onPaymentCompleted(12, 40, null);
    }

    @Test
    @DisplayName("PaymentRejected pasa el outcome y el motivo tal cual")
    void paymentRejectedDelega() {
        send("PaymentRejected", Map.of("reservationId", 12, "paymentId", 40,
                "outcome", "RETRY_ALLOWED", "rejectionReason", "Comprobante ilegible"));

        verify(useCases).onPaymentRejected(12, 40, "RETRY_ALLOWED", "Comprobante ilegible");
    }

    @Test
    @DisplayName("InvoiceGenerated y MaintenanceCompleted delegan con sus campos")
    void otrosEventos() {
        send("InvoiceGenerated", Map.of("reservationId", 12, "invoiceId", 90, "paymentId", 40));
        send("MaintenanceCompleted", Map.of("personId", 7, "maintenanceId", 5, "vehicleName", "Kia Picanto"));

        verify(useCases).onInvoiceGenerated(12, 90, 40);
        verify(useCases).onMaintenanceCompleted(7, 5, "Kia Picanto");
    }

    @Test
    @DisplayName("Los ids se aceptan también como texto numérico")
    void idsComoTexto() {
        send("PaymentSubmitted", Map.of("reservationId", "12", "paymentId", 40L));

        verify(useCases).onPaymentSubmitted(12, 40);
    }

    @Test
    @DisplayName("Falta un campo obligatorio: 400 con el campo exacto y no llama al caso de uso")
    void campoFaltante() {
        assertThatThrownBy(() -> send("PaymentSubmitted", Map.of("paymentId", 40)))
                .isInstanceOf(InvalidEventException.class)
                .hasFieldOrPropertyWithValue("code", "INVALID_EVENT")
                .hasFieldOrPropertyWithValue("field", "payload.reservationId");

        verifyNoInteractions(useCases);
    }

    @Test
    @DisplayName("Un id con decimales no es válido")
    void idConDecimales() {
        assertThatThrownBy(() -> send("PaymentSubmitted", Map.of("reservationId", 12.5, "paymentId", 40)))
                .isInstanceOf(InvalidEventException.class)
                .hasFieldOrPropertyWithValue("field", "payload.reservationId");

        verifyNoInteractions(useCases);
    }

    @Test
    @DisplayName("Un tipo de evento desconocido responde UNKNOWN_EVENT_TYPE")
    void tipoDesconocido() {
        assertThatThrownBy(() -> send("PaymentRefunded", Map.of("reservationId", 12)))
                .isInstanceOf(InvalidEventException.class)
                .hasFieldOrPropertyWithValue("code", "UNKNOWN_EVENT_TYPE");

        verifyNoInteractions(useCases);
    }
}

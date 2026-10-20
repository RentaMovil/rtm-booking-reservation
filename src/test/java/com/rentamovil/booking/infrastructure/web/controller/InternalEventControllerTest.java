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

/** POST /internal/events: validates the structure and delegates to PaymentEventUseCases. */
class InternalEventControllerTest {

    private final PaymentEventUseCases useCases = mock(PaymentEventUseCases.class);
    private final InternalEventController controller = new InternalEventController(useCases);

    private InternalEventResponse send(String type, Map<String, Object> payload) {
        return controller.receive(new InternalEventRequest("evt-1", type, payload));
    }

    @Test
    @DisplayName("PaymentSubmitted delegates to onPaymentSubmitted and responds PROCESSED")
    void paymentSubmittedDelegates() {
        when(useCases.onPaymentSubmitted(12, 40)).thenReturn(true);

        InternalEventResponse response = send("PaymentSubmitted", Map.of("reservationId", 12, "paymentId", 40));

        assertThat(response.getResult()).isEqualTo(InternalEventResponse.PROCESSED);
        assertThat(response.getType()).isEqualTo("PaymentSubmitted");
        assertThat(response.getEventId()).isEqualTo("evt-1");
    }

    @Test
    @DisplayName("An already-applied event responds DUPLICATE_IGNORED (200, not an error)")
    void repeatedEvent() {
        when(useCases.onPaymentCompleted(12, 40, 3)).thenReturn(false);

        InternalEventResponse response = send("PaymentCompleted",
                Map.of("reservationId", 12, "paymentId", 40, "actorUserId", 3));

        assertThat(response.getResult()).isEqualTo(InternalEventResponse.DUPLICATE_IGNORED);
    }

    @Test
    @DisplayName("actorUserId is optional in PaymentCompleted")
    void actorIsOptional() {
        send("PaymentCompleted", Map.of("reservationId", 12, "paymentId", 40));

        verify(useCases).onPaymentCompleted(12, 40, null);
    }

    @Test
    @DisplayName("PaymentRejected passes the outcome and the reason as-is")
    void paymentRejectedDelegates() {
        send("PaymentRejected", Map.of("reservationId", 12, "paymentId", 40,
                "outcome", "RETRY_ALLOWED", "rejectionReason", "Illegible receipt"));

        verify(useCases).onPaymentRejected(12, 40, "RETRY_ALLOWED", "Illegible receipt");
    }

    @Test
    @DisplayName("InvoiceGenerated and MaintenanceCompleted delegate with their fields")
    void otherEvents() {
        send("InvoiceGenerated", Map.of("reservationId", 12, "invoiceId", 90, "paymentId", 40));
        send("MaintenanceCompleted", Map.of("personId", 7, "maintenanceId", 5, "vehicleName", "Kia Picanto"));

        verify(useCases).onInvoiceGenerated(12, 90, 40);
        verify(useCases).onMaintenanceCompleted(7, 5, "Kia Picanto");
    }

    @Test
    @DisplayName("Ids are also accepted as numeric text")
    void idsAsText() {
        send("PaymentSubmitted", Map.of("reservationId", "12", "paymentId", 40L));

        verify(useCases).onPaymentSubmitted(12, 40);
    }

    @Test
    @DisplayName("Missing a required field: 400 with the exact field, and the use case is never called")
    void missingField() {
        assertThatThrownBy(() -> send("PaymentSubmitted", Map.of("paymentId", 40)))
                .isInstanceOf(InvalidEventException.class)
                .hasFieldOrPropertyWithValue("code", "INVALID_EVENT")
                .hasFieldOrPropertyWithValue("field", "payload.reservationId");

        verifyNoInteractions(useCases);
    }

    @Test
    @DisplayName("An id with decimals is not valid")
    void idWithDecimals() {
        assertThatThrownBy(() -> send("PaymentSubmitted", Map.of("reservationId", 12.5, "paymentId", 40)))
                .isInstanceOf(InvalidEventException.class)
                .hasFieldOrPropertyWithValue("field", "payload.reservationId");

        verifyNoInteractions(useCases);
    }

    @Test
    @DisplayName("An unknown event type responds UNKNOWN_EVENT_TYPE")
    void unknownEventType() {
        assertThatThrownBy(() -> send("PaymentRefunded", Map.of("reservationId", 12)))
                .isInstanceOf(InvalidEventException.class)
                .hasFieldOrPropertyWithValue("code", "UNKNOWN_EVENT_TYPE");

        verifyNoInteractions(useCases);
    }
}

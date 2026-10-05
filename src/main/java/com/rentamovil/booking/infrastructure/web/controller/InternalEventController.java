package com.rentamovil.booking.infrastructure.web.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rentamovil.booking.domain.port.in.PaymentEventUseCases;
import com.rentamovil.booking.infrastructure.web.dto.InternalEventPayload;
import com.rentamovil.booking.infrastructure.web.dto.InternalEventRequest;
import com.rentamovil.booking.infrastructure.web.dto.InternalEventResponse;
import com.rentamovil.booking.infrastructure.web.exception.InvalidEventException;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Adaptador de entrada para los eventos de otros servicios (payment-billing, fleet-maintenance).
 *
 * <p>Solo traduce el mensaje y delega en {@link PaymentEventUseCases}: las reglas (transiciones,
 * notificaciones, idempotencia) viven en el caso de uso. Si más adelante los eventos llegan por un
 * broker, se agrega un listener que llame a los mismos casos de uso y este controlador puede
 * quedarse o retirarse.
 *
 * <p><b>Sin JWT a propósito</b>, igual que /internal/reservations: lo llama otro servicio, no una
 * persona. El api-gateway no enruta /internal/**, así que no es alcanzable desde afuera.
 *
 * <p>Mientras payment-billing no exista, este endpoint permite probar el flujo de pago completo
 * con Postman: PaymentSubmitted y luego PaymentCompleted o PaymentRejected.
 */
@Slf4j
@RestController
@RequestMapping("/internal/events")
@RequiredArgsConstructor
public class InternalEventController {

    private final PaymentEventUseCases paymentEvents;

    @PostMapping
    public InternalEventResponse receive(@Valid @RequestBody InternalEventRequest request) {
        InternalEventPayload payload = new InternalEventPayload(request.getPayload());

        boolean applied = switch (request.getEventType()) {
            case "PaymentSubmitted" -> paymentEvents.onPaymentSubmitted(
                    payload.requiredInt("reservationId"),
                    payload.requiredInt("paymentId"));
            case "PaymentCompleted" -> paymentEvents.onPaymentCompleted(
                    payload.requiredInt("reservationId"),
                    payload.requiredInt("paymentId"),
                    payload.optionalInt("actorUserId"));
            case "PaymentRejected" -> paymentEvents.onPaymentRejected(
                    payload.requiredInt("reservationId"),
                    payload.requiredInt("paymentId"),
                    payload.requiredText("outcome"),
                    payload.optionalText("rejectionReason"));
            case "InvoiceGenerated" -> paymentEvents.onInvoiceGenerated(
                    payload.requiredInt("reservationId"),
                    payload.requiredInt("invoiceId"),
                    payload.requiredInt("paymentId"));
            case "MaintenanceCompleted" -> paymentEvents.onMaintenanceCompleted(
                    payload.requiredInt("personId"),
                    payload.requiredInt("maintenanceId"),
                    payload.requiredText("vehicleName"));
            default -> throw new InvalidEventException("UNKNOWN_EVENT_TYPE", "type",
                    "Tipo de evento desconocido: " + request.getEventType());
        };

        String result = applied ? InternalEventResponse.PROCESSED : InternalEventResponse.DUPLICATE_IGNORED;
        log.info("Evento {} (eventId={}): {}", request.getEventType(), request.getEventId(), result);
        return new InternalEventResponse(request.getEventType(), request.getEventId(), result);
    }
}

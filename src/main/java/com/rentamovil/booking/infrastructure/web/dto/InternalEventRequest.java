package com.rentamovil.booking.infrastructure.web.dto;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAlias;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Evento que otro servicio le envía a booking por POST /internal/events.
 *
 * <pre>
 * {
 *   "eventId": "7f1c…",               (opcional: solo para trazabilidad en los logs)
 *   "eventType": "PaymentCompleted",
 *   "payload": { "reservationId": 12, "paymentId": 40, "actorUserId": 3 }
 * }
 * </pre>
 *
 * <p>"eventType" es el nombre del sobre que usan todos los servicios (fleet incluido); "type" se
 * sigue aceptando para no romper a quien ya lo envía así. Los campos que exige cada tipo los
 * valida {@code InternalEventPayload}.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class InternalEventRequest {

    private String eventId;

    @NotBlank(message = "obligatorio")
    @JsonAlias("type")
    private String eventType;

    @NotNull(message = "obligatorio")
    private Map<String, Object> payload;
}

package com.rentamovil.booking.infrastructure.fleet;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.rentamovil.booking.domain.port.out.DomainEventPublisherPort;

/**
 * Lleva RentalStarted y RentalCompleted a fleet-maintenance (POST /internal/events), que con eso
 * pone el vehículo en RENTED o AVAILABLE.
 *
 * <p>Se registra como un listener más del dispatcher en proceso, así los casos de uso siguen
 * publicando igual. Mismo sobre que usan todos los servicios: eventId, eventType, occurredAt,
 * payload. fleet es idempotente, así que reenviar un evento no hace daño.
 *
 * <p>Si fleet está caído el evento se pierde y queda en el log con su eventId: no hay outbox
 * todavía. El pickup o la devolución en booking no se deshacen por eso.
 */
@Component
public class FleetEventForwarder {

    private static final Logger log = LoggerFactory.getLogger(FleetEventForwarder.class);

    private final RestClient restClient;

    public FleetEventForwarder(DomainEventPublisherPort events,
                               @Value("${app.booking-reservation.fleet-url}") String fleetUrl,
                               @Value("${app.booking-reservation.fleet-timeout}") Duration timeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        this.restClient = RestClient.builder().baseUrl(fleetUrl).requestFactory(requestFactory).build();

        events.subscribe("RentalStarted", this::forward);
        events.subscribe("RentalCompleted", this::forward);
    }

    void forward(String eventType, Object payload) {
        String eventId = UUID.randomUUID().toString();
        Map<String, Object> envelope = Map.of(
                "eventId", eventId,
                "eventType", eventType,
                "occurredAt", Instant.now().toString(),
                "payload", payload);
        try {
            restClient.post()
                    .uri("/internal/events")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(envelope)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Evento {} enviado a fleet-maintenance (eventId={})", eventType, eventId);
        } catch (RestClientException e) {
            log.error("No se pudo enviar {} a fleet-maintenance (eventId={}, payload={})",
                    eventType, eventId, payload, e);
        }
    }
}

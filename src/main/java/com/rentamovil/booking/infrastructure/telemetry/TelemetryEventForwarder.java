package com.rentamovil.booking.infrastructure.telemetry;

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
 * Lleva RentalStarted y RentalCompleted a telemetry-gps (POST /internal/events), que con eso
 * empieza o deja de esperar reportes de ese gpsId (Location.INV-001).
 *
 * <p>Igual que FleetEventForwarder: un listener más del dispatcher en proceso, mismo sobre
 * (eventId, eventType, occurredAt, payload). Si telemetry-gps está caído el evento se pierde y
 * queda en el log con su eventId: no hay outbox todavía, mismo nivel que el resto de los
 * forwarders del sistema.
 */
@Component
public class TelemetryEventForwarder {

    private static final Logger log = LoggerFactory.getLogger(TelemetryEventForwarder.class);

    private final RestClient restClient;

    public TelemetryEventForwarder(DomainEventPublisherPort events,
                                   @Value("${app.booking-reservation.telemetry-url}") String telemetryUrl,
                                   @Value("${app.booking-reservation.telemetry-timeout}") Duration timeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        this.restClient = RestClient.builder().baseUrl(telemetryUrl).requestFactory(requestFactory).build();

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
            log.info("Evento {} enviado a telemetry-gps (eventId={})", eventType, eventId);
        } catch (RestClientException e) {
            log.error("No se pudo enviar {} a telemetry-gps (eventId={}, payload={})",
                    eventType, eventId, payload, e);
        }
    }
}

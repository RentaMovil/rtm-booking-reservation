package com.rentamovil.booking.domain.port.out;

/**
 * Publica los eventos de dominio. Sin broker a esta escala (5 servicios en una máquina): la
 * implementación es un dispatcher en proceso que notifica a los adaptadores registrados.
 * Ver {@code 09-microservices/service-catalog.md} → "async events, in-process".
 */
public interface DomainEventPublisherPort {

    void publish(String eventType, Object payload);

    /** Registra a quién notificar cuando ocurra un evento. Sin esto no hay broker todavía. */
    void subscribe(String eventType, EventListener listener);

    @FunctionalInterface
    interface EventListener {
        void onEvent(String eventType, Object payload);
    }
}
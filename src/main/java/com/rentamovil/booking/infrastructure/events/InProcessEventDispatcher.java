package com.rentamovil.booking.infrastructure.events;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.rentamovil.booking.domain.port.out.DomainEventPublisherPort;

/**
 * Dispatcher de eventos en proceso.
 *
 * <p>No hay broker a esta escala (5 servicios en una sola máquina, ver service-catalog.md). Este
 * adaptador notifica a los listeners registrados en memoria. Cuando haya broker, se sustituye por
 * un adaptador Kafka/RabbitMQ y los casos de uso no cambian.
 *
 * <p>Un listener que lanza no detiene a los demás: cada reacción a evento es independiente y una
 * notificación que falle no debe tumbar la reserva que la originó.
 */
@Component
public class InProcessEventDispatcher implements DomainEventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(InProcessEventDispatcher.class);

    private final Map<String, List<EventListener>> listeners = new ConcurrentHashMap<>();

    @Override
    public void subscribe(String eventType, EventListener listener) {
        listeners.computeIfAbsent(eventType, key -> new CopyOnWriteArrayList<>()).add(listener);
    }

    @Override
    public void publish(String eventType, Object payload) {
        List<EventListener> registered = listeners.get(eventType);
        if (registered == null || registered.isEmpty()) {
            // Normal al principio: los servicios que consumen este evento todavía no existen.
            log.debug("Evento {} publicado sin listeners registrados", eventType);
            return;
        }
        for (EventListener listener : registered) {
            try {
                listener.onEvent(eventType, payload);
            } catch (Exception e) {
                log.error("Un listener de {} falló; los demás listeners siguen", eventType, e);
            }
        }
    }
}
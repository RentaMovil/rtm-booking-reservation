package com.rentamovil.booking.infrastructure.web.realtime;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.rentamovil.booking.domain.entity.Notification;
import com.rentamovil.booking.domain.port.out.RealtimeNotificationPort;
import com.rentamovil.booking.infrastructure.web.dto.AdminNotificationEvent;
import com.rentamovil.booking.infrastructure.web.dto.NotificationResponse;

/**
 * Registro de conexiones SSE en memoria (una sola instancia del servicio; no hay varias réplicas
 * detrás del gateway). Un emitter por pestaña/dispositivo, se limpian solos al cerrarse.
 */
@Component
public class SseNotificationBroadcaster implements RealtimeNotificationPort {

    private static final long EMITTER_TIMEOUT_MS = 0L; // sin timeout: lo corta el cliente o la red

    private final Map<Integer, List<SseEmitter>> userEmitters = new ConcurrentHashMap<>();
    private final List<SseEmitter> adminEmitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe(Integer personId, boolean admin) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        List<SseEmitter> emitters = userEmitters.computeIfAbsent(personId, id -> new CopyOnWriteArrayList<>());
        emitters.add(emitter);
        if (admin) {
            adminEmitters.add(emitter);
        }

        Runnable cleanup = () -> {
            emitters.remove(emitter);
            adminEmitters.remove(emitter);
        };
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(ex -> cleanup.run());

        // Sin esto el navegador no dispara el "open" del EventSource hasta el primer evento real.
        try {
            emitter.send(SseEmitter.event().name("ready").data("ok"));
        } catch (IOException ex) {
            cleanup.run();
        }
        return emitter;
    }

    @Override
    public void pushToUser(Notification notification) {
        send(userEmitters.get(notification.getPersonId()), "notification",
                NotificationResponse.from(notification));
    }

    @Override
    public void pushToAdmins(String type, String message, Integer reservationId) {
        send(adminEmitters, "admin-notification", new AdminNotificationEvent(type, message, reservationId));
    }

    private void send(List<SseEmitter> emitters, String eventName, Object payload) {
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : List.copyOf(emitters)) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(payload));
            } catch (IOException | IllegalStateException ex) {
                emitter.complete();
                emitters.remove(emitter);
                adminEmitters.remove(emitter);
            }
        }
    }
}

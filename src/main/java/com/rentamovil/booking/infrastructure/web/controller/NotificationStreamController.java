package com.rentamovil.booking.infrastructure.web.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.rentamovil.booking.domain.entity.AuthenticatedUser;
import com.rentamovil.booking.infrastructure.web.auth.CurrentUser;
import com.rentamovil.booking.infrastructure.web.realtime.SseNotificationBroadcaster;

import lombok.RequiredArgsConstructor;

/**
 * Canal SSE de la campanita: una notificación ya creada se ve sin recargar.
 *
 * <p>EventSource no puede mandar el header Authorization, así que este token viaja por query param
 * (ver CurrentUserArgumentResolver) solo para esta ruta.
 */
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationStreamController {

    private final SseNotificationBroadcaster broadcaster;

    @GetMapping(path = "/stream", produces = "text/event-stream")
    public SseEmitter stream(@CurrentUser AuthenticatedUser currentUser) {
        return broadcaster.subscribe(currentUser.getPersonId(), currentUser.isAdmin());
    }
}

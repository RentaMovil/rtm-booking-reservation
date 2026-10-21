package com.rentamovil.booking.infrastructure.web.dto;

import java.time.LocalDateTime;

import lombok.Getter;

/** Aviso en vivo para administradores: no tiene fila en notification (ver RealtimeNotificationPort). */
@Getter
public class AdminNotificationEvent {

    private final String type;
    private final String message;
    private final Integer reservationId;
    private final LocalDateTime sentDate;

    public AdminNotificationEvent(String type, String message, Integer reservationId) {
        this.type = type;
        this.message = message;
        this.reservationId = reservationId;
        this.sentDate = LocalDateTime.now();
    }
}

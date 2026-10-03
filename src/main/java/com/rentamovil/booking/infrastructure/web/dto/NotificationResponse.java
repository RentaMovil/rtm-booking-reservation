package com.rentamovil.booking.infrastructure.web.dto;

import java.time.LocalDateTime;

import com.rentamovil.booking.domain.entity.Notification;
import com.rentamovil.booking.domain.enums.NotificationType;

import lombok.Getter;

/** Respuesta de una notificación. */
@Getter
public class NotificationResponse {

    private final Integer id;
    private final NotificationType type;
    private final String message;
    private final LocalDateTime sentDate;
    private final boolean read;
    private final Integer reservationId;

    private NotificationResponse(Integer id, NotificationType type, String message,
                                 LocalDateTime sentDate, boolean read, Integer reservationId) {
        this.id = id;
        this.type = type;
        this.message = message;
        this.sentDate = sentDate;
        this.read = read;
        this.reservationId = reservationId;
    }

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(notification.getId(), notification.getType(),
                notification.getMessage(), notification.getSentDate(), notification.isRead(),
                notification.getReservationId());
    }
}
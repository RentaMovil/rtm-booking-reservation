package com.rentamovil.booking.domain.port.in;

import java.util.List;

import com.rentamovil.booking.domain.entity.Notification;

/** HU-NOTIF-001: la campanita de notificaciones in-app. */
public interface NotificationUseCases {

    List<Notification> findMine(Integer personId);

    /** Marca una notificación como leída. Solo la puede marcar su propio destinatario. */
    Notification markAsRead(Integer notificationId, Integer personId);
}
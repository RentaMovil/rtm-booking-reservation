package com.rentamovil.booking.domain.port.out;

import com.rentamovil.booking.domain.entity.Notification;

/**
 * Empuje en vivo de notificaciones (SSE) a quien esté conectado en ese momento.
 *
 * <p>Complementa a {@link NotificationRepositoryPort}, no lo reemplaza: las notificaciones del
 * cliente siguen quedando en la tabla (se leen con GET /notifications aunque no haya estado
 * conectado); el aviso a administradores es solo en vivo, sin fila por admin, porque
 * notification.person_id no tiene con qué representar "para cualquier admin conectado".
 */
public interface RealtimeNotificationPort {

    /** Empuja al dueño de la notificación, si tiene una conexión abierta. */
    void pushToUser(Notification notification);

    /** Empuja a todos los administradores conectados en ese momento. */
    void pushToAdmins(String type, String message, Integer reservationId);
}

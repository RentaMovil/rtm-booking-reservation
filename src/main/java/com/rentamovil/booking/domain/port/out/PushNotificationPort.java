package com.rentamovil.booking.domain.port.out;

import com.rentamovil.booking.domain.entity.Notification;

/**
 * Push al celular del dueño de la notificación (banner del sistema operativo, aunque la app esté
 * cerrada). Complementa a {@link RealtimeNotificationPort} (SSE, solo si hay una pestaña/app
 * abierta ahora mismo) y a {@link NotificationRepositoryPort} (la fila siempre queda guardada,
 * esto es solo el aviso).
 */
public interface PushNotificationPort {

    /** Si el envío falla (iam caído, Expo caído, el usuario no tiene ningún dispositivo
     * registrado) no debe propagar la excepción: la notificación ya quedó guardada igual. */
    void sendToUser(Notification notification);
}

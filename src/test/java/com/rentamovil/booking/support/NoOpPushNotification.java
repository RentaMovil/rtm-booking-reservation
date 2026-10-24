package com.rentamovil.booking.support;

import com.rentamovil.booking.domain.entity.Notification;
import com.rentamovil.booking.domain.port.out.PushNotificationPort;

/** Fake de PushNotificationPort: los tests de casos de uso no verifican el push al celular. */
public class NoOpPushNotification implements PushNotificationPort {

    @Override
    public void sendToUser(Notification notification) {
        // no-op
    }
}

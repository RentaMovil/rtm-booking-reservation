package com.rentamovil.booking.support;

import com.rentamovil.booking.domain.entity.Notification;
import com.rentamovil.booking.domain.port.out.RealtimeNotificationPort;

/** Fake de RealtimeNotificationPort: los tests de casos de uso no verifican el push en vivo. */
public class NoOpRealtimeNotification implements RealtimeNotificationPort {

    @Override
    public void pushToUser(Notification notification) {
        // no-op
    }

    @Override
    public void pushToAdmins(String type, String message, Integer reservationId) {
        // no-op
    }
}

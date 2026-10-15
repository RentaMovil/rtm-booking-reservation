package com.rentamovil.booking.domain.entity;

import java.time.LocalDateTime;

import com.rentamovil.booking.domain.enums.NotificationType;

import lombok.Getter;

/**
 * Una notificación in-app.
 *
 * <p>Las cuatro referencias (reservation/invoice/payment/maintenance) son nullable y cada fila usa
 * exactamente una: dice de qué registro trata la notificación. El {@link NotificationType} dice
 * de qué tipo de evento salió — complementarios, no redundantes.
 */
@Getter
public class Notification {

    private final Integer id;
    private final Integer personId;
    private final NotificationType type;
    private final String message;
    private final LocalDateTime sentDate;
    /** Lo abrió el RECIPIENTE, no "se entregó sin error" (06-data/models.md). */
    private final boolean read;
    private final Integer reservationId;
    private final Integer invoiceId;
    private final Integer paymentId;
    private final Integer maintenanceId;

    private Notification(Integer id, Integer personId, NotificationType type, String message,
                         LocalDateTime sentDate, boolean read, Integer reservationId,
                         Integer invoiceId, Integer paymentId, Integer maintenanceId) {
        this.id = id;
        this.personId = personId;
        this.type = type;
        this.message = message;
        this.sentDate = sentDate;
        this.read = read;
        this.reservationId = reservationId;
        this.invoiceId = invoiceId;
        this.paymentId = paymentId;
        this.maintenanceId = maintenanceId;
    }

    /** Notificación sobre una reserva. */
    public static Notification aboutReservation(Integer personId, NotificationType type, String message,
                                                Integer reservationId) {
        return new Notification(null, personId, type, message, LocalDateTime.now(), false,
                reservationId, null, null, null);
    }

    /** Notificación sobre un pago o su factura. */
    public static Notification aboutPayment(Integer personId, NotificationType type, String message,
                                            Integer reservationId, Integer paymentId, Integer invoiceId) {
        return new Notification(null, personId, type, message, LocalDateTime.now(), false,
                reservationId, invoiceId, paymentId, null);
    }

    /** Notificación sobre mantenimiento de un vehículo (HU-NOTIF-001). */
    public static Notification aboutMaintenance(Integer personId, NotificationType type, String message,
                                                Integer maintenanceId) {
        return new Notification(null, personId, type, message, LocalDateTime.now(), false,
                null, null, null, maintenanceId);
    }

    public static Notification restore(Integer id, Integer personId, NotificationType type, String message,
                                       LocalDateTime sentDate, boolean read, Integer reservationId,
                                       Integer invoiceId, Integer paymentId, Integer maintenanceId) {
        return new Notification(id, personId, type, message, sentDate, read, reservationId,
                invoiceId, paymentId, maintenanceId);
    }

    public Notification markAsRead() {
        if (this.read) {
            return this;
        }
        return new Notification(id, personId, type, message, sentDate, true, reservationId,
                invoiceId, paymentId, maintenanceId);
    }
}
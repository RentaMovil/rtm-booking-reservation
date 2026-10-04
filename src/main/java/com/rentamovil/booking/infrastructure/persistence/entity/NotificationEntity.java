package com.rentamovil.booking.infrastructure.persistence.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Mapeo de booking.notification. */
@Entity
@Table(name = "notification")
@Getter
@Setter
@NoArgsConstructor
public class NotificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Integer id;

    @Column(name = "person_id", nullable = false)
    private Integer personId;

    // String, no el enum: el mapper guarda getType().name() y lee con NotificationType.valueOf().
    @Column(name = "notification_type", nullable = false, length = 50)
    private String notificationType;

    @Column(name = "message", nullable = false)
    private String message;

    @Column(name = "sent_date", nullable = false)
    private LocalDateTime sentDate;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "reservation_id")
    private Integer reservationId;

    @Column(name = "invoice_id")
    private Integer invoiceId;

    @Column(name = "payment_id")
    private Integer paymentId;

    @Column(name = "maintenance_id")
    private Integer maintenanceId;
}
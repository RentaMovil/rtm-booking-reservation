package com.rentamovil.booking.domain.port.out;

import java.util.List;
import java.util.Optional;

import com.rentamovil.booking.domain.entity.Notification;

/** Lo que el dominio necesita del almacenamiento de notificaciones. */
public interface NotificationRepositoryPort {

    Notification save(Notification notification);

    Optional<Notification> findById(Integer id);

    /** HU-NOTIF-001: notificaciones de una persona, más recientes primero. */
    List<Notification> findByPersonId(Integer personId);

    /** Para el badge de la campanita. */
    long countUnread(Integer personId);

    /** Si ya se notificó esta factura (InvoiceGenerated repetido). */
    boolean existsByInvoiceId(Integer invoiceId);

    /** Si ya se notificó este mantenimiento a esta persona (MaintenanceCompleted repetido). */
    boolean existsByMaintenanceIdAndPersonId(Integer maintenanceId, Integer personId);

    // NOTE: events.md dice que se usa el eventId como clave de idempotencia para no duplicar
    // notificaciones. Eso necesita una tabla de eventos procesados, que 06-data/models.md no
    // define — y models.md es la fuente de verdad del esquema. Mientras no exista, las reacciones
    // a eventos deduplican por estado de la reserva y por invoice_id / maintenance_id.
}
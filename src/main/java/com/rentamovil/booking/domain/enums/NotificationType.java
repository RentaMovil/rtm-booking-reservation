package com.rentamovil.booking.domain.enums;

/**
 * Tipos de notificación (06-data/models.md -> notification).
 *
 * <p>Un valor por evento de dominio que genera notificación. NO hay valor para
 * {@code InvoiceGenerated}: se agrupa bajo {@link #PAYMENT_APPROVED} porque aprobar un pago
 * genera la factura en el mismo momento (ver events.md del servicio).
 */
public enum NotificationType {

    RESERVATION_CREATED,
    RESERVATION_CANCELLED,
    RESERVATION_RETURN_BRANCH_CHANGED,
    /**
     * Ya no se emite: la fecha de devolución no se puede modificar. Se conserva porque el CHECK
     * de booking.notification lo admite y puede haber filas antiguas con este valor.
     */
    RESERVATION_RETURN_DATE_EXTENDED,
    RESERVATION_EXPIRED,
    PAYMENT_APPROVED,
    PAYMENT_REJECTED,
    MAINTENANCE_COMPLETED
}
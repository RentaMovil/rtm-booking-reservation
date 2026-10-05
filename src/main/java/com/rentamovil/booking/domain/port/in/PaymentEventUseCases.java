package com.rentamovil.booking.domain.port.in;

/**
 * Reacciones a los eventos de otros servicios (events.md → "Eventos consumidos").
 *
 * <p>La garantía de entrega es at-least-once: el mismo evento puede llegar dos veces. Cada método
 * devuelve {@code true} si aplicó el cambio y {@code false} si el evento ya estaba aplicado y se
 * ignoró. Un evento repetido no cambia el estado ni vuelve a notificar.
 *
 * <p>La deduplicación es por estado (y por invoice_id / maintenance_id en las notificaciones),
 * no por eventId: guardar los eventos procesados necesitaría una tabla que 06-data/models.md no
 * define.
 */
public interface PaymentEventUseCases {

    /**
     * PaymentSubmitted: el cliente subió el comprobante. PENDING_PAYMENT → PENDING_REVIEW.
     * Es el paso previo obligatorio a PaymentCompleted.
     */
    boolean onPaymentSubmitted(Integer reservationId, Integer paymentId);

    /** PaymentCompleted: PENDING_REVIEW → CONFIRMED. */
    boolean onPaymentCompleted(Integer reservationId, Integer paymentId, Integer actorUserId);

    /**
     * PaymentRejected: según outcome, PENDING_REVIEW → PENDING_PAYMENT (RETRY_ALLOWED) o
     * → CANCELLED (RESERVATION_CANCELLED). INV-016.
     */
    boolean onPaymentRejected(Integer reservationId, Integer paymentId, String outcome,
                              String rejectionReason);

    /** InvoiceGenerated: la factura va en la notificación de pago aprobado. */
    boolean onInvoiceGenerated(Integer reservationId, Integer invoiceId, Integer paymentId);

    /** MaintenanceCompleted de fleet-maintenance. */
    boolean onMaintenanceCompleted(Integer personId, Integer maintenanceId, String vehicleName);
}

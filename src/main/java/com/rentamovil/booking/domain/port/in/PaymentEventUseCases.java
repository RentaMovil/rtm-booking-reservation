package com.rentamovil.booking.domain.port.in;

/**
 * Reacciones a los eventos de otros servicios (events.md → "Eventos consumidos").
 *
 * <p>Cada uno es idempotente: la garantía de entrega es at-least-once y el mismo evento puede
 * llegar dos veces.
 */
public interface PaymentEventUseCases {

    /** PaymentCompleted: PENDING_REVIEW → CONFIRMED. */
    void onPaymentCompleted(Integer reservationId, Integer paymentId, Integer actorUserId);

    /**
     * PaymentRejected: según outcome, PENDING_REVIEW → PENDING_PAYMENT (RETRY_ALLOWED) o
     * → CANCELLED (RESERVATION_CANCELLED). INV-016.
     */
    void onPaymentRejected(Integer reservationId, Integer paymentId, String outcome,
                           String rejectionReason);

    /** InvoiceGenerated: la factura va en la notificación de pago aprobado. */
    void onInvoiceGenerated(Integer reservationId, Integer invoiceId, Integer paymentId);

    /** MaintenanceCompleted de fleet-maintenance. */
    void onMaintenanceCompleted(Integer personId, Integer maintenanceId, String vehicleName);
}
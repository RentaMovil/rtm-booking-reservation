package com.rentamovil.booking.domain.enums;

/**
 * Estados de una reserva (02-domain/entities-and-rules.md).
 *
 * <p>Las transiciones permitidas están en entities-and-rules.md y en
 * {@code Reservation.canTransitionTo}; la tabla de estados en SQL Server valida el valor.
 */
public enum ReservationStatus {

    /** Reserva creada, esperando que el cliente suba el comprobante de pago. */
    PENDING_PAYMENT,

    /** Comprobante subido, esperando la revisión del Administrador. */
    PENDING_REVIEW,

    /** Pago aprobado: la reserva es válida. */
    CONFIRMED,

    /** Cancelada, sin penalización. Estado terminal. */
    CANCELLED,

    /** El alquiler ya terminó. Estado terminal. */
    COMPLETED
}
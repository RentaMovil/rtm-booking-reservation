package com.rentamovil.booking.domain.enums;

/**
 * Estados de una renta (06-data/models.md -> rental).
 *
 * <p>Solo dos: una renta empieza cuando el Administrador registra el pickup y termina cuando
 * registra la devolución.
 */
public enum RentalStatus {

    /** El vehículo está en manos del titular. actual_end_date y final_mileage aún no existen. */
    IN_PROGRESS,

    /** El Administrador registró la devolución. */
    COMPLETED
}
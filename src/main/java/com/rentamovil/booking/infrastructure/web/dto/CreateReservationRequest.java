package com.rentamovil.booking.infrastructure.web.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

/**
 * HU-BOOKING-003: datos de una reserva nueva.
 *
 * <p>No incluye clientId: el titular es el usuario autenticado y su id sale del token.
 */
@Getter
public class CreateReservationRequest {

    @NotNull(message = "obligatorio")
    private Integer vehicleId;

    /** Opcional: el seguro no es obligatorio en una reserva (INV-006). */
    private Integer insuranceTypeId;

    @NotNull(message = "obligatorio")
    private Integer pickupBranchId;

    /** INV-010: puede ser distinta de la de recogida, sin recargo. */
    @NotNull(message = "obligatorio")
    private Integer returnBranchId;

    @NotNull(message = "obligatorio")
    private LocalDateTime startDate;

    @NotNull(message = "obligatorio")
    private LocalDateTime endDate;

    /** HU-BOOKING-007: hay que aceptar los términos antes de confirmar. */
    @NotNull(message = "obligatorio")
    private Boolean termsAccepted;
}
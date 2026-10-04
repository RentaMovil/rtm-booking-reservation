package com.rentamovil.booking.infrastructure.web.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import lombok.Getter;

/**
 * HU-BOOKING-005: modificar una reserva. Solo se puede cambiar la sucursal de devolución
 * (INV-013: hasta 3 días antes de end_date, lo valida el dominio).
 *
 * <p>newEndDate existe solo para rechazarlo con un mensaje claro: la fecha de devolución no se
 * modifica. Sin este campo, Jackson ignoraría el valor en silencio y el cliente creería que el
 * cambio se aplicó.
 */
@Getter
public class ModifyReservationRequest {

    @Null(message = "la fecha de devolución no se puede modificar; solo la sucursal de devolución")
    private LocalDateTime newEndDate;

    @NotNull(message = "obligatorio")
    private Integer newReturnBranchId;
}

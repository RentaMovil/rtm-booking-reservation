package com.rentamovil.booking.infrastructure.web.dto;

import java.time.LocalDateTime;

import lombok.Getter;

/**
 * HU-BOOKING-005: modificar una reserva. Solo se toca lo que venga informado.
 *
 * <p>INV-003 permite ampliar endDate pero nunca acortarlo; INV-013 permite cambiar returnBranchId
 * hasta 3 días antes de end_date. Los dos los valida el dominio.
 */
@Getter
public class ModifyReservationRequest {

    private LocalDateTime newEndDate;

    private Integer newReturnBranchId;
}
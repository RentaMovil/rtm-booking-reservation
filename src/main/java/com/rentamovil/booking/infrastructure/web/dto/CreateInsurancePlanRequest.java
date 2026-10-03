package com.rentamovil.booking.infrastructure.web.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

/** HU-BOOKING-009: alta de un plan de seguro. Solo Administradores. */
@Getter
public class CreateInsurancePlanRequest {

    @NotBlank(message = "obligatorio")
    private String name;

    private String coverageDetails;

    @NotNull(message = "obligatorio")
    @DecimalMin(value = "0.0", message = "no puede ser negativo")
    private BigDecimal dailyCost;
}
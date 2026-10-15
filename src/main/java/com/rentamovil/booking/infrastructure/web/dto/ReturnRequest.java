package com.rentamovil.booking.infrastructure.web.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

/** HU-RENTAL-002: registro de la devolución. */
@Getter
public class ReturnRequest {

    @NotNull(message = "obligatorio")
    private BigDecimal finalMileage;
}
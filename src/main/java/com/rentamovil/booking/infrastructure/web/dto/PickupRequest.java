package com.rentamovil.booking.infrastructure.web.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

/**
 * HU-RENTAL-001: registro del pickup.
 *
 * <p>gpsId viene aquí y no se pide a telemetry-gps: el Administrador sabe qué dispositivo
 * físico está instalando (ver decisions.md del servicio).
 */
@Getter
public class PickupRequest {

    @NotNull(message = "obligatorio")
    private Integer gpsId;

    @NotNull(message = "obligatorio")
    private BigDecimal initialMileage;
}
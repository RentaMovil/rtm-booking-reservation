package com.rentamovil.booking.domain.port.out;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Datos del vehículo y disponibilidad, que pertenecen al contexto Fleet & Maintenance.
 *
 * <p>Lo implementa {@code FleetVehicleClientAdapter}: GET fleet-maintenance:3002/vehicles/{id}
 * (precio) y /vehicles/{id}/availability?from=Y&to=Z.
 */
public interface VehicleAvailabilityPort {

    /** Si el vehículo existe y está disponible para el rango pedido. */
    boolean isAvailable(Integer vehicleId, LocalDateTime startDate, LocalDateTime endDate);

    /** Precio diario del vehículo en el rango pedido. */
    BigDecimal dailyRate(Integer vehicleId, LocalDateTime startDate, LocalDateTime endDate);
}
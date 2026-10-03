package com.rentamovil.booking.domain.port.out;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Datos del vehículo y disponibilidad, que pertenecen al contexto Fleet & Maintenance.
 *
 * <p>El servicio dueño todavía no existe: no hay adaptador REST posible. Ver
 * {@code PendingVehicleAvailabilityAdapter}, que falla con un mensaje explícito en vez de
 * inventar un precio.
 *
 * <p>Cuando exista: GET fleet-maintenance:3002/vehicles/{id}/availability?from=Y&to=Z
 */
public interface VehicleAvailabilityPort {

    /** Si el vehículo existe y está disponible para el rango pedido. */
    boolean isAvailable(Integer vehicleId, LocalDateTime startDate, LocalDateTime endDate);

    /** Precio diario del vehículo en el rango pedido. */
    BigDecimal dailyRate(Integer vehicleId, LocalDateTime startDate, LocalDateTime endDate);
}
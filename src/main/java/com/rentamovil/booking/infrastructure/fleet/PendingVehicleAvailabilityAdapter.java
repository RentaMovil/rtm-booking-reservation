package com.rentamovil.booking.infrastructure.fleet;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.rentamovil.booking.domain.exception.BusinessRuleException;
import com.rentamovil.booking.domain.port.out.VehicleAvailabilityPort;

/**
 * PENDIENTE: sustituir por un cliente REST cuando exista el servicio fleet-maintenance.
 *
 * <p>Aún no se puede consultar la base: vehicle, branch y el precio del vehículo pertenecen al
 * contexto Fleet &amp; Maintenance, que no existe. Este adaptador falla con un mensaje claro en
 * lugar de devolver un precio inventado, para que ningún test o demo pase por alto que el importe
 * real todavía no se puede calcular.
 *
 * <p>Cuando exista fleet: GET http://fleet-maintenance:3002/vehicles/{id}/availability?from&to
 */
@Component
public class PendingVehicleAvailabilityAdapter implements VehicleAvailabilityPort {

    private static final Logger log = LoggerFactory.getLogger(PendingVehicleAvailabilityAdapter.class);

    @Override
    public boolean isAvailable(Integer vehicleId, LocalDateTime startDate, LocalDateTime endDate) {
        throw fleetNotReady(vehicleId);
    }

    @Override
    public BigDecimal dailyRate(Integer vehicleId, LocalDateTime startDate, LocalDateTime endDate) {
        throw fleetNotReady(vehicleId);
    }

    private BusinessRuleException fleetNotReady(Integer vehicleId) {
        log.warn("Se consultó disponibilidad/precio del vehículo {}: fleet-maintenance aún no existe", vehicleId);
        return new BusinessRuleException("FLEET_SERVICE_UNAVAILABLE",
                "El catálogo de vehículos todavía no está disponible");
    }
}
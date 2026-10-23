package com.rentamovil.booking.infrastructure.web.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rentamovil.booking.domain.port.in.ReservationUseCases;
import com.rentamovil.booking.infrastructure.web.dto.VehicleOccupancyResponse;

import lombok.RequiredArgsConstructor;

/**
 * Endpoint interno para fleet-maintenance (ver README → "Dependencia síncrona").
 *
 * <p><b>Sin JWT a propósito</b>: no hay una persona detrás, es un servicio consultando a otro.
 * No se expone fuera de la red interna — el api-gateway no debe enrutar /internal/*.
 */
@RestController
@RequestMapping("/internal/reservations")
@RequiredArgsConstructor
public class InternalReservationController {

    private final ReservationUseCases reservations;

    /**
     * ¿Está este vehículo ocupado en ese rango? fleet-maintenance lo usa para el filtro del
     * catálogo y para bloquear el retiro de un vehículo con una reserva futura.
     *
     * <p>available viene del propio servicio, no de una comparación en el cliente: fleet no
     * debería tener que saber si un CANCELLED ocupa.
     */
    @GetMapping("/vehicle-occupancy")
    public VehicleOccupancyResponse vehicleOccupancy(
            @RequestParam Integer vehicleId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        // El contrato (booking-reservation.yaml) usa fechas sin hora: se cubren los días completos,
        // incluido el último, para no dar por libre un vehículo que se entrega ese mismo día
        if (to.isBefore(from)) {
            throw new com.rentamovil.booking.domain.exception.InvalidDateRangeException(
                    "INVALID_DATE_RANGE", "La fecha final no puede ser anterior a la inicial");
        }

        var ranges = reservations.vehicleOccupancy(vehicleId, from.atStartOfDay(), to.plusDays(1).atStartOfDay());
        return new VehicleOccupancyResponse(vehicleId, ranges.isEmpty(),
                ranges.stream()
                        .map(r -> new VehicleOccupancyResponse.OccupiedRange(
                                r.reservationId(), r.startDate(), r.endDate()))
                        .toList());
    }
}
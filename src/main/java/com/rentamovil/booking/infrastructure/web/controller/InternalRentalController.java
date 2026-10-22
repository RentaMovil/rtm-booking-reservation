package com.rentamovil.booking.infrastructure.web.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rentamovil.booking.domain.port.in.RentalUseCases;
import com.rentamovil.booking.infrastructure.web.dto.InternalRentalResponse;

import lombok.RequiredArgsConstructor;

/**
 * Endpoint interno para telemetry-gps (ver su README -> "Dependencia síncrona"): resuelve
 * rentalId -> gpsId y la ventana de fechas de la renta, para GET /tracking/{rentalId}/live|history.
 *
 * <p><b>Sin JWT a propósito</b>, igual que /internal/reservations/vehicle-occupancy: no hay una
 * persona detrás, es un servicio consultando a otro. No se expone fuera de la red interna — el
 * api-gateway no debe enrutar /internal/*.
 */
@RestController
@RequestMapping("/internal/rentals")
@RequiredArgsConstructor
public class InternalRentalController {

    private final RentalUseCases rentals;

    @GetMapping("/{id}")
    public InternalRentalResponse findById(@PathVariable Integer id) {
        return InternalRentalResponse.from(rentals.findById(id));
    }

    /**
     * Para que telemetry-gps reconstruya su estado en memoria al arrancar (ActiveTrackingPort no
     * sobrevive un reinicio): qué gpsId tiene una renta IN_PROGRESS ahora mismo.
     */
    @GetMapping("/active")
    public List<InternalRentalResponse> findActive() {
        return rentals.findInProgress().stream().map(InternalRentalResponse::from).toList();
    }
}

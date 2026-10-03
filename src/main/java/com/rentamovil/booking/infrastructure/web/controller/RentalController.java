package com.rentamovil.booking.infrastructure.web.controller;

import java.time.LocalDateTime;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rentamovil.booking.domain.entity.AuthenticatedUser;
import com.rentamovil.booking.domain.exception.ForbiddenException;
import com.rentamovil.booking.domain.port.in.RentalUseCases;
import com.rentamovil.booking.infrastructure.web.auth.CurrentUser;
import com.rentamovil.booking.infrastructure.web.dto.PickupRequest;
import com.rentamovil.booking.infrastructure.web.dto.RentalResponse;
import com.rentamovil.booking.infrastructure.web.dto.ReturnRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Pickup y devolución: HU-RENTAL-001 y HU-RENTAL-002. Solo Administradores — son acciones
 * físicas sobre el vehículo.
 */
@RestController
@RequestMapping("/rentals")
@RequiredArgsConstructor
public class RentalController {

    private final RentalUseCases rentals;

    /**
     * HU-RENTAL-001: el Administrador registra el pickup de una reserva CONFIRMED.
     * El gpsId viene en el body: sabe qué dispositivo está instalando.
     */
    @PostMapping("/{reservationId}/pickup")
    public RentalResponse pickup(@CurrentUser AuthenticatedUser currentUser,
                                 @PathVariable Integer reservationId,
                                 @Valid @RequestBody PickupRequest request) {
        requireAdmin(currentUser);
        return RentalResponse.from(rentals.startPickup(reservationId, request.getGpsId(),
                LocalDateTime.now(), request.getInitialMileage(), currentUser.getUserId()));
    }

    /** HU-RENTAL-002: registra la devolución y cierra el ciclo (reserva → COMPLETED). */
    @PostMapping("/{id}/return")
    public RentalResponse completeReturn(@CurrentUser AuthenticatedUser currentUser,
                                        @PathVariable Integer id,
                                        @Valid @RequestBody ReturnRequest request) {
        requireAdmin(currentUser);
        return RentalResponse.from(rentals.completeReturn(id, LocalDateTime.now(),
                request.getFinalMileage(), currentUser.getUserId()));
    }

    private void requireAdmin(AuthenticatedUser currentUser) {
        if (!currentUser.isAdmin()) {
            throw new ForbiddenException("Necesitas permisos de administrador");
        }
    }
}
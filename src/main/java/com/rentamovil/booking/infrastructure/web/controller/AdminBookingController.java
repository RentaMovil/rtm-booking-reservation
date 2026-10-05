package com.rentamovil.booking.infrastructure.web.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rentamovil.booking.domain.entity.AuthenticatedUser;
import com.rentamovil.booking.domain.enums.ReservationStatus;
import com.rentamovil.booking.domain.exception.ForbiddenException;
import com.rentamovil.booking.domain.port.in.RentalUseCases;
import com.rentamovil.booking.domain.port.in.ReservationUseCases;
import com.rentamovil.booking.infrastructure.web.auth.CurrentUser;
import com.rentamovil.booking.infrastructure.web.dto.RentalResponse;
import com.rentamovil.booking.infrastructure.web.dto.ReservationResponse;

import lombok.RequiredArgsConstructor;

/**
 * Listados de administración: HU-BOOKING-008 (reservas), las rentas abiertas y el historial
 * completo de rentas.
 *
 * <p><b>Por qué NO están bajo /admin/**:</b> el api-gateway solo enruta
 * /reservations/**, /rentals/** y /notifications/** hacia este servicio. Una ruta
 * /admin/** llegaría al gateway y saldría con 404, porque no hay ninguna regla que la cubra.
 * El gateway es la autoridad de las rutas públicas, así que todo endpoint que deba consumirse
 * desde el navegador tiene que caer bajo uno de esos tres prefijos.
 *
 * <p>Los endpoints con "/admin" o "/in-progress" dentro del prefijo no son un chequeo de
 * seguridad: el permiso real lo verifica booking contra el rol del token (y el backend responde
 * 403 igual). Es solo para que un CLIENT no los encuentre por accidente al listar.
 */
@RestController
@RequiredArgsConstructor
public class AdminBookingController {

    private final ReservationUseCases reservations;
    private final RentalUseCases rentals;

    /** HU-BOOKING-008: todas las reservas, con filtro opcional por estado. */
    @GetMapping("/reservations/admin")
    public List<ReservationResponse> listReservations(@CurrentUser AuthenticatedUser currentUser,
                                                      @RequestParam(required = false) ReservationStatus status) {
        requireAdmin(currentUser);
        return reservations.findAll(status).stream().map(ReservationResponse::from).toList();
    }

    /** Las rentas abiertas, para saber qué vehículos están fuera. */
    @GetMapping("/rentals/in-progress")
    public List<RentalResponse> listRentals(@CurrentUser AuthenticatedUser currentUser) {
        requireAdmin(currentUser);
        return rentals.findInProgress().stream().map(RentalResponse::from).toList();
    }

    /** Todas las rentas, incluidas las cerradas (COMPLETED), más recientes primero. */
    @GetMapping("/rentals/admin")
    public List<RentalResponse> listAllRentals(@CurrentUser AuthenticatedUser currentUser) {
        requireAdmin(currentUser);
        return rentals.findAll().stream().map(RentalResponse::from).toList();
    }

    private void requireAdmin(AuthenticatedUser currentUser) {
        if (!currentUser.isAdmin()) {
            throw new ForbiddenException("Necesitas permisos de administrador");
        }
    }
}
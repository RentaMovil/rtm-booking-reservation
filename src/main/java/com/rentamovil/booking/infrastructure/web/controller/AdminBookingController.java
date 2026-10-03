package com.rentamovil.booking.infrastructure.web.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rentamovil.booking.domain.entity.AuthenticatedUser;
import com.rentamovil.booking.domain.enums.ReservationStatus;
import com.rentamovil.booking.domain.exception.ForbiddenException;
import com.rentamovil.booking.domain.port.in.InsuranceUseCases;
import com.rentamovil.booking.domain.port.in.RentalUseCases;
import com.rentamovil.booking.domain.port.in.ReservationUseCases;
import com.rentamovil.booking.infrastructure.web.auth.CurrentUser;
import com.rentamovil.booking.infrastructure.web.dto.CreateInsurancePlanRequest;
import com.rentamovil.booking.infrastructure.web.dto.InsurancePlanResponse;
import com.rentamovil.booking.infrastructure.web.dto.RentalResponse;
import com.rentamovil.booking.infrastructure.web.dto.ReservationResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Panel de administración: HU-BOOKING-008 (reservas), HU-BOOKING-009 (seguros) y el listado de
 * rentas abiertas.
 *
 * <p>El permiso se comprueba aquí, no en el gateway: el gateway avisa pero cada servicio valida
 * por su cuenta (decisions.md de rtm-iam).
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminBookingController {

    private final ReservationUseCases reservations;
    private final InsuranceUseCases insurance;
    private final RentalUseCases rentals;

    /** HU-BOOKING-008: todas las reservas, con filtro opcional por estado. */
    @GetMapping("/reservations")
    public List<ReservationResponse> listReservations(@CurrentUser AuthenticatedUser currentUser,
                                                      @RequestParam(required = false) ReservationStatus status) {
        requireAdmin(currentUser);
        return reservations.findAll(status).stream().map(ReservationResponse::from).toList();
    }

    /** Las rentas abiertas, para saber qué vehículos están fuera. */
    @GetMapping("/rentals")
    public List<RentalResponse> listRentals(@CurrentUser AuthenticatedUser currentUser) {
        requireAdmin(currentUser);
        return rentals.findInProgress().stream().map(RentalResponse::from).toList();
    }

    /** HU-BOOKING-009: crear un plan de seguro. */
    @PostMapping("/insurance-plans")
    public InsurancePlanResponse createPlan(@CurrentUser AuthenticatedUser currentUser,
                                            @Valid @RequestBody CreateInsurancePlanRequest request) {
        requireAdmin(currentUser);
        return InsurancePlanResponse.from(insurance.createPlan(
                request.getName(), request.getCoverageDetails(), request.getDailyCost()));
    }

    private void requireAdmin(AuthenticatedUser currentUser) {
        if (!currentUser.isAdmin()) {
            throw new ForbiddenException("Necesitas permisos de administrador");
        }
    }
}
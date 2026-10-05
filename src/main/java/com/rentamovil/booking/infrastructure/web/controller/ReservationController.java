package com.rentamovil.booking.infrastructure.web.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rentamovil.booking.domain.entity.AuthenticatedUser;
import com.rentamovil.booking.domain.exception.ResourceNotFoundException;
import com.rentamovil.booking.domain.port.in.ReservationUseCases;
import com.rentamovil.booking.infrastructure.web.auth.CurrentUser;
import com.rentamovil.booking.infrastructure.web.dto.CreateReservationRequest;
import com.rentamovil.booking.infrastructure.web.dto.ModifyReservationRequest;
import com.rentamovil.booking.infrastructure.web.dto.ReservationResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Reservas del cliente autenticado: HU-BOOKING-003 (crear), 004 (listar/ver), 005 (modificar),
 * 006 (cancelar).
 *
 * <p>El titular sale del token (personId), nunca de la URL ni del cuerpo.
 */
@RestController
@RequestMapping("/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationUseCases reservations;

    /** HU-BOOKING-003: crea la reserva. Nace en PENDING_PAYMENT. */
    @PostMapping
    public ResponseEntity<ReservationResponse> create(@CurrentUser AuthenticatedUser currentUser,
                                                     @Valid @RequestBody CreateReservationRequest request) {
        ReservationResponse response = ReservationResponse.from(reservations.create(
                currentUser.getPersonId(),
                request.getVehicleId(),
                request.getInsuranceTypeId(),
                request.getPickupBranchId(),
                request.getReturnBranchId(),
                request.getStartDate(),
                request.getEndDate(),
                Boolean.TRUE.equals(request.getTermsAccepted())));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** HU-BOOKING-004: "Mis reservas". INV-011: pueden coexistir varias activas. */
    @GetMapping("/mine")
    public List<ReservationResponse> findMine(@CurrentUser AuthenticatedUser currentUser) {
        return reservations.findMine(currentUser.getPersonId())
                .stream()
                .map(ReservationResponse::from)
                .toList();
    }

    /** Detalle de una reserva propia. Un Administrador puede ver cualquiera. */
    @GetMapping("/{id}")
    public ReservationResponse findById(@CurrentUser AuthenticatedUser currentUser, @PathVariable Integer id) {
        var reservation = reservations.findById(id);
        if (!reservation.getClientId().equals(currentUser.getPersonId()) && !currentUser.isAdmin()) {
            // 404 y no 403: no se confirma que exista una reserva con ese id.
            throw new ResourceNotFoundException("La reserva no existe");
        }
        return ReservationResponse.from(reservation);
    }

    /**
     * HU-BOOKING-005: cambiar la sucursal de devolución (INV-013). Es lo único modificable de una
     * reserva; enviar newEndDate responde 400.
     */
    @PatchMapping("/{id}")
    public ReservationResponse modify(@CurrentUser AuthenticatedUser currentUser,
                                      @PathVariable Integer id,
                                      @Valid @RequestBody ModifyReservationRequest request) {
        return ReservationResponse.from(reservations.changeReturnBranch(id, currentUser.getPersonId(),
                request.getNewReturnBranchId()));
    }

    /** HU-BOOKING-006: cancelar. INV-004: hasta 3 días antes. INV-005: sin recargo. */
    @PostMapping("/{id}/cancel")
    public ReservationResponse cancel(@CurrentUser AuthenticatedUser currentUser,
                                      @PathVariable Integer id) {
        return ReservationResponse.from(reservations.cancel(id, currentUser.getPersonId()));
    }
}
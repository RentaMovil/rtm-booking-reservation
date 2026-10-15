package com.rentamovil.booking.infrastructure.web.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rentamovil.booking.domain.entity.AuthenticatedUser;
import com.rentamovil.booking.domain.exception.ForbiddenException;
import com.rentamovil.booking.domain.port.in.InsuranceUseCases;
import com.rentamovil.booking.infrastructure.web.auth.CurrentUser;
import com.rentamovil.booking.infrastructure.web.dto.CreateInsurancePlanRequest;
import com.rentamovil.booking.infrastructure.web.dto.InsurancePlanResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * HU-BOOKING-002 (cotizar un seguro) y HU-BOOKING-009 (gestionar planes).
 *
 * <p><b>Por qué cuelga de /reservations:</b> el api-gateway no enruta /insurance-types aunque
 * el contrato OpenAPI lo declare; solo enruta /reservations/**, /rentals/** y /notifications/**.
 * El gateway manda sobre el contrato, así que los planes de seguro viven aquí.
 *
 * <p>El GET es público a propósito: el cliente necesita ver precios antes de tener sesión para
 * reservar. INV-012 exige sesión para reservar, no para mirar precios.
 */
@RestController
@RequestMapping("/reservations/insurance-types")
@RequiredArgsConstructor
public class InsuranceController {

    private final InsuranceUseCases insurance;

    /**
     * Con startDate y endDate devuelve el precio ya calculado para ese rango (INV-007: el seguro
     * se cobra una vez por la duración de la reserva). Sin fechas, solo el precio diario.
     */
    @GetMapping
    public List<InsurancePlanResponse> listPlans(@RequestParam(required = false) LocalDateTime startDate,
                                                @RequestParam(required = false) LocalDateTime endDate) {
        boolean hasRange = startDate != null && endDate != null;
        return insurance.listPlans()
                .stream()
                .map(plan -> hasRange
                        ? InsurancePlanResponse.from(plan, insurance.quote(plan.getId(), startDate, endDate))
                        : InsurancePlanResponse.from(plan))
                .toList();
    }

    /** HU-BOOKING-009: crear un plan de seguro. Solo Administradores. */
    @PostMapping
    public InsurancePlanResponse createPlan(@CurrentUser AuthenticatedUser currentUser,
                                            @Valid @RequestBody CreateInsurancePlanRequest request) {
        if (!currentUser.isAdmin()) {
            throw new ForbiddenException("Necesitas permisos de administrador");
        }
        return InsurancePlanResponse.from(insurance.createPlan(
                request.getName(), request.getCoverageDetails(), request.getDailyCost()));
    }
}
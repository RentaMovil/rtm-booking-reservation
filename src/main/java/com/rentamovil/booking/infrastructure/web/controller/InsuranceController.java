package com.rentamovil.booking.infrastructure.web.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rentamovil.booking.domain.port.in.InsuranceUseCases;
import com.rentamovil.booking.infrastructure.web.dto.InsurancePlanResponse;

import lombok.RequiredArgsConstructor;

/**
 * HU-BOOKING-002: los planes de seguro y su precio para las fechas que se consulten.
 *
 * <p>Público a propósito: el cliente los necesita antes de tener sesión para reservar, igual que
 * puede navegar el catálogo sin estar registrado (INV-012 dice que reservar exige sesión, no
 * mirar precios).
 */
@RestController
@RequestMapping("/insurance-plans")
@RequiredArgsConstructor
public class InsuranceController {

    private final InsuranceUseCases insurance;

    @GetMapping
    public List<InsurancePlanResponse> listPlans(
            @RequestParam(required = false) LocalDateTime startDate,
            @RequestParam(required = false) LocalDateTime endDate) {
        boolean hasRange = startDate != null && endDate != null;
        return insurance.listPlans()
                .stream()
                .map(plan -> hasRange
                        ? InsurancePlanResponse.from(plan, insurance.quote(plan.getId(), startDate, endDate))
                        : InsurancePlanResponse.from(plan))
                .toList();
    }
}
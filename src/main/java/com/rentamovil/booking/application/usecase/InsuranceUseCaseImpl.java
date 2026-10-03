package com.rentamovil.booking.application.usecase;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import com.rentamovil.booking.domain.entity.InsuranceType;
import com.rentamovil.booking.domain.exception.InvalidDateRangeException;
import com.rentamovil.booking.domain.exception.ResourceNotFoundException;
import com.rentamovil.booking.domain.port.in.InsuranceUseCases;
import com.rentamovil.booking.domain.port.out.InsuranceTypeRepositoryPort;

/** HU-BOOKING-002 (cotizar un seguro) y HU-BOOKING-009 (gestionar planes). */
public class InsuranceUseCaseImpl implements InsuranceUseCases {

    private final InsuranceTypeRepositoryPort insuranceTypeRepository;

    public InsuranceUseCaseImpl(InsuranceTypeRepositoryPort insuranceTypeRepository) {
        this.insuranceTypeRepository = insuranceTypeRepository;
    }

    @Override
    public List<InsuranceType> listPlans() {
        return insuranceTypeRepository.findAll();
    }

    @Override
    public BigDecimal quote(Integer insuranceTypeId, LocalDateTime startDate, LocalDateTime endDate) {
        InsuranceType plan = insuranceTypeRepository.findById(insuranceTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("El plan de seguro no existe"));

        if (startDate == null || endDate == null || !endDate.isAfter(startDate)) {
            throw new InvalidDateRangeException("INVALID_DATE_RANGE",
                    "La fecha de devolución debe ser posterior a la de recogida");
        }

        long days = Math.max(Duration.between(startDate, endDate).toDays(), 1);
        return plan.costFor(days);
    }

    @Override
    public InsuranceType createPlan(String name, String coverageDetails, BigDecimal dailyCost) {
        return insuranceTypeRepository.save(InsuranceType.create(name, coverageDetails, dailyCost));
    }
}
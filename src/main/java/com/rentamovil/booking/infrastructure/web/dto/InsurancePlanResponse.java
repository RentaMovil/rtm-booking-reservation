package com.rentamovil.booking.infrastructure.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.rentamovil.booking.domain.entity.InsuranceType;

import lombok.Getter;

/** Un plan de seguro con su precio ya calculado para el rango pedido. */
@Getter
public class InsurancePlanResponse {

    private final Integer id;
    private final String name;
    private final String coverageDetails;
    private final BigDecimal dailyCost;
    /** Precio total del rango consultado. Null si la consulta no pasó fechas. */
    private final BigDecimal totalCost;

    public InsurancePlanResponse(Integer id, String name, String coverageDetails, BigDecimal dailyCost,
                                 BigDecimal totalCost) {
        this.id = id;
        this.name = name;
        this.coverageDetails = coverageDetails;
        this.dailyCost = dailyCost;
        this.totalCost = totalCost;
    }

    public static InsurancePlanResponse from(InsuranceType plan) {
        return new InsurancePlanResponse(plan.getId(), plan.getName(), plan.getCoverageDetails(),
                plan.getDailyCost(), null);
    }

    public static InsurancePlanResponse from(InsuranceType plan, BigDecimal totalCost) {
        return new InsurancePlanResponse(plan.getId(), plan.getName(), plan.getCoverageDetails(),
                plan.getDailyCost(), totalCost);
    }
}
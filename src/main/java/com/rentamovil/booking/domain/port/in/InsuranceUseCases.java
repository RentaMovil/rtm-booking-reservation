package com.rentamovil.booking.domain.port.in;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.rentamovil.booking.domain.entity.InsuranceType;

/** HU-BOOKING-002 y HU-BOOKING-009: los planes de seguro y su precio. */
public interface InsuranceUseCases {

    /** Los planes que el cliente puede elegir. */
    List<InsuranceType> listPlans();

    /** Precio para un rango concreto (INV-007: se cobra una vez por la duración). */
    BigDecimal quote(Integer insuranceTypeId, LocalDateTime startDate, LocalDateTime endDate);

    InsuranceType createPlan(String name, String coverageDetails, BigDecimal dailyCost);
}
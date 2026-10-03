package com.rentamovil.booking.domain.entity;

import java.math.BigDecimal;

import com.rentamovil.booking.domain.exception.BusinessRuleException;

import lombok.Getter;

/**
 * Un plan de seguro que el cliente puede añadir a una reserva.
 *
 * <p>INV-006: como máximo uno por reserva. INV-007: se cobra una vez por la duración total de la
 * reserva, no por día extra si luego se amplía.
 */
@Getter
public class InsuranceType {

    private final Integer id;
    private final String name;
    private final String coverageDetails;
    private final BigDecimal dailyCost;

    private InsuranceType(Integer id, String name, String coverageDetails, BigDecimal dailyCost) {
        this.id = id;
        this.name = name;
        this.coverageDetails = coverageDetails;
        this.dailyCost = dailyCost;
    }

    public static InsuranceType create(String name, String coverageDetails, BigDecimal dailyCost) {
        if (name == null || name.isBlank()) {
            throw new BusinessRuleException("INVALID_INSURANCE", "El nombre del seguro es obligatorio");
        }
        if (dailyCost == null || dailyCost.signum() < 0) {
            throw new BusinessRuleException("INVALID_INSURANCE", "El costo diario no puede ser negativo");
        }
        return new InsuranceType(null, name, coverageDetails, dailyCost);
    }

    public static InsuranceType restore(Integer id, String name, String coverageDetails,
                                        BigDecimal dailyCost) {
        return new InsuranceType(id, name, coverageDetails, dailyCost);
    }

    /**
     * Costo del seguro para toda la duración del alquiler.
     *
     * <p>Se redondea a 2 decimales porque la columna es DECIMAL(10,2): multiplicar sin redondear
     * dejaría centavos que luego no caben en la columna y el insert fallaría.
     */
    public BigDecimal costFor(long rentalDays) {
        if (rentalDays <= 0) {
            throw new BusinessRuleException("INVALID_DATE_RANGE", "La reserva debe durar al menos un día");
        }
        return dailyCost.multiply(BigDecimal.valueOf(rentalDays))
                .setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
package com.rentamovil.booking.infrastructure.persistence.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Mapeo de booking.insurance_type. */
@Entity
@Table(name = "insurance_type")
@Getter
@Setter
@NoArgsConstructor
public class InsuranceTypeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "insurance_type_id")
    private Integer id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "coverage_details")
    private String coverageDetails;

    @Column(name = "daily_cost", nullable = false, precision = 10, scale = 2)
    private BigDecimal dailyCost;
}
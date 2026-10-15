package com.rentamovil.booking.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rentamovil.booking.infrastructure.persistence.entity.InsuranceTypeEntity;

/** Acceso Spring Data a booking.insurance_type. */
public interface InsuranceTypeJpaRepository extends JpaRepository<InsuranceTypeEntity, Integer> {

    List<InsuranceTypeEntity> findAllByOrderByDailyCostAsc();

    Optional<InsuranceTypeEntity> findByNameIgnoreCase(String name);
}
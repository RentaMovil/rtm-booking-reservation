package com.rentamovil.booking.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rentamovil.booking.infrastructure.persistence.entity.AuditEntity;

/**
 * Acceso Spring Data a shared.audit. Expone save() pero no find*: booking_app solo tiene
 * permiso de INSERT sobre esa tabla, así que una lectura fallaría en tiempo de ejecución.
 */
public interface AuditJpaRepository extends JpaRepository<AuditEntity, Integer> {
}
package com.rentamovil.booking.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rentamovil.booking.infrastructure.persistence.entity.NotificationEntity;

/** Acceso Spring Data a booking.notification. */
public interface NotificationJpaRepository extends JpaRepository<NotificationEntity, Integer> {

    List<NotificationEntity> findByPersonIdOrderBySentDateDesc(Integer personId);

    Optional<NotificationEntity> findByIdAndPersonId(Integer id, Integer personId);

    long countByPersonIdAndReadFalse(Integer personId);
}
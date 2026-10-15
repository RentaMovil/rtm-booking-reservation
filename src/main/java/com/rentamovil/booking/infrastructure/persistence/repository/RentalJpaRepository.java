package com.rentamovil.booking.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rentamovil.booking.infrastructure.persistence.entity.RentalEntity;

/** Acceso Spring Data a booking.rental. */
public interface RentalJpaRepository extends JpaRepository<RentalEntity, Integer> {

    Optional<RentalEntity> findByReservationId(Integer reservationId);

    List<RentalEntity> findByStatusOrderByActualStartDateAsc(String status);

    List<RentalEntity> findAllByOrderByActualStartDateDesc();
}
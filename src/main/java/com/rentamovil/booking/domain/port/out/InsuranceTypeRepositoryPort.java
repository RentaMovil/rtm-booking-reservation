package com.rentamovil.booking.domain.port.out;

import java.util.List;
import java.util.Optional;

import com.rentamovil.booking.domain.entity.InsuranceType;

/** Lo que el dominio necesita del almacenamiento de planes de seguro. */
public interface InsuranceTypeRepositoryPort {

    InsuranceType save(InsuranceType insuranceType);

    Optional<InsuranceType> findById(Integer id);

    /** HU-BOOKING-002: los planes que el cliente puede elegir. */
    List<InsuranceType> findAll();
}
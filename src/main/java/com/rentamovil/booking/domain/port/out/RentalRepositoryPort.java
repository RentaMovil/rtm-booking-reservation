package com.rentamovil.booking.domain.port.out;

import java.util.List;
import java.util.Optional;

import com.rentamovil.booking.domain.entity.Rental;

/** Lo que el dominio necesita del almacenamiento de rentas. */
public interface RentalRepositoryPort {

    Rental save(Rental rental);

    Optional<Rental> findById(Integer id);

    Optional<Rental> findByReservationId(Integer reservationId);

    /** HU-RENTAL-002: las rentas aún abiertas (para el panel de administración). */
    List<Rental> findInProgress();
}
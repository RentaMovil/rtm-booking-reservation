package com.rentamovil.booking.domain.port.in;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.rentamovil.booking.domain.entity.Rental;

/** HU-RENTAL-001 y HU-RENTAL-002: registrar pickup y devolución. */
public interface RentalUseCases {

    /**
     * HU-RENTAL-001: el Administrador registra el pickup de una reserva CONFIRMED.
     * El gpsId viene en el body — el Administrador sabe qué dispositivo está instalando.
     * Emite RentalStarted.
     */
    Rental startPickup(Integer reservationId, Integer gpsId, LocalDateTime pickupDate,
                       BigDecimal initialMileage, Integer actorUserId);

    /** HU-RENTAL-002: registra la devolución. Emite RentalCompleted y cierra la reserva. */
    Rental completeReturn(Integer rentalId, LocalDateTime returnDate, BigDecimal finalMileage,
                          Integer actorUserId);

    Rental findById(Integer id);

    /** Las rentas abiertas, para el panel de administración. */
    List<Rental> findInProgress();

    /** Todas las rentas, incluidas las cerradas: el historial del Administrador. */
    List<Rental> findAll();
}
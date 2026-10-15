package com.rentamovil.booking.infrastructure.web.dto;

import java.time.LocalDateTime;

import com.rentamovil.booking.domain.entity.Rental;

import lombok.Getter;

/**
 * Lo que telemetry-gps necesita para resolver rentalId -> gpsId + la ventana de fechas de la
 * renta (su README, "Dependencia síncrona"). Nunca se regenera ni cambia de forma: el resto de
 * los campos de Rental no le sirven a quien consume esto.
 */
@Getter
public class InternalRentalResponse {

    private final Integer id;
    private final Integer gpsId;
    private final LocalDateTime actualStartDate;
    private final LocalDateTime actualEndDate;

    private InternalRentalResponse(Integer id, Integer gpsId, LocalDateTime actualStartDate,
                                   LocalDateTime actualEndDate) {
        this.id = id;
        this.gpsId = gpsId;
        this.actualStartDate = actualStartDate;
        this.actualEndDate = actualEndDate;
    }

    public static InternalRentalResponse from(Rental rental) {
        return new InternalRentalResponse(rental.getId(), rental.getGpsId(), rental.getActualStartDate(),
                rental.getActualEndDate());
    }
}

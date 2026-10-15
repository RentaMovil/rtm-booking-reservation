package com.rentamovil.booking.infrastructure.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.rentamovil.booking.domain.entity.Rental;
import com.rentamovil.booking.domain.enums.RentalStatus;

import lombok.Getter;

/** Respuesta de una renta. */
@Getter
public class RentalResponse {

    private final Integer id;
    private final Integer reservationId;
    private final Integer gpsId;
    private final LocalDateTime actualStartDate;
    private final LocalDateTime actualEndDate;
    private final BigDecimal initialMileage;
    private final BigDecimal finalMileage;
    private final RentalStatus status;

    private RentalResponse(Integer id, Integer reservationId, Integer gpsId,
                           LocalDateTime actualStartDate, LocalDateTime actualEndDate,
                           BigDecimal initialMileage, BigDecimal finalMileage, RentalStatus status) {
        this.id = id;
        this.reservationId = reservationId;
        this.gpsId = gpsId;
        this.actualStartDate = actualStartDate;
        this.actualEndDate = actualEndDate;
        this.initialMileage = initialMileage;
        this.finalMileage = finalMileage;
        this.status = status;
    }

    public static RentalResponse from(Rental rental) {
        return new RentalResponse(rental.getId(), rental.getReservationId(), rental.getGpsId(),
                rental.getActualStartDate(), rental.getActualEndDate(), rental.getInitialMileage(),
                rental.getFinalMileage(), rental.getStatus());
    }
}
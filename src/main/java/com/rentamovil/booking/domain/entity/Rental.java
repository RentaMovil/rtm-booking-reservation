package com.rentamovil.booking.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.rentamovil.booking.domain.enums.RentalStatus;
import com.rentamovil.booking.domain.exception.BusinessRuleException;

import lombok.Getter;

/**
 * La ejecución física de una reserva confirmada: pickup → devolución.
 *
 * <p>Existe solo desde que el Administrador registra el pickup (HU-RENTAL-001). Una reserva
 * CONFIRMED sin fila en rental simplemente todavía no se recogió — no hay una renta "vacía"
 * creada por adelantado (modeling decision 3 de 06-data/models.md).
 */
@Getter
public class Rental {

    private final Integer id;
    private final Integer reservationId;
    private final Integer vehicleId;
    /** Dispositivo GPS instalado por el Administrador. No se asigna vía llamada a telemetry. */
    private final Integer gpsId;
    private final LocalDateTime actualStartDate;
    private final LocalDateTime actualEndDate;
    private final BigDecimal initialMileage;
    private final BigDecimal finalMileage;
    private final RentalStatus status;

    private Rental(Integer id, Integer reservationId, Integer vehicleId, Integer gpsId,
                   LocalDateTime actualStartDate, LocalDateTime actualEndDate,
                   BigDecimal initialMileage, BigDecimal finalMileage, RentalStatus status) {
        this.id = id;
        this.reservationId = reservationId;
        this.vehicleId = vehicleId;
        this.gpsId = gpsId;
        this.actualStartDate = actualStartDate;
        this.actualEndDate = actualEndDate;
        this.initialMileage = initialMileage;
        this.finalMileage = finalMileage;
        this.status = status;
    }

    /** HU-RENTAL-001: el Administrador registra el pickup. Empieza en IN_PROGRESS. */
    public static Rental startPickup(Integer reservationId, Integer vehicleId, Integer gpsId,
                                     LocalDateTime pickupDate, BigDecimal initialMileage) {
        if (initialMileage == null || initialMileage.signum() < 0) {
            throw new BusinessRuleException("INVALID_MILEAGE", "El kilometraje inicial es obligatorio");
        }
        return new Rental(null, reservationId, vehicleId, gpsId, pickupDate, null,
                initialMileage, null, RentalStatus.IN_PROGRESS);
    }

    public static Rental restore(Integer id, Integer reservationId, Integer vehicleId, Integer gpsId,
                                 LocalDateTime actualStartDate, LocalDateTime actualEndDate,
                                 BigDecimal initialMileage, BigDecimal finalMileage, RentalStatus status) {
        return new Rental(id, reservationId, vehicleId, gpsId, actualStartDate, actualEndDate,
                initialMileage, finalMileage, status);
    }

    /**
     * HU-RENTAL-002: el Administrador registra la devolución.
     *
     * <p>El kilometraje final no puede ser menor que el inicial: un vehículo que vuelve con menos
     * kilómetros que al salir significa que se registró mal.
     */
    public Rental completeReturn(LocalDateTime returnDate, BigDecimal finalMileage) {
        if (this.status == RentalStatus.COMPLETED) {
            return this;
        }
        if (finalMileage == null || finalMileage.signum() < 0) {
            throw new BusinessRuleException("INVALID_MILEAGE", "El kilometraje final es obligatorio");
        }
        if (finalMileage.compareTo(this.initialMileage) < 0) {
            throw new BusinessRuleException("INVALID_MILEAGE",
                    "El kilometraje final no puede ser menor que el inicial");
        }
        return new Rental(id, reservationId, vehicleId, gpsId, actualStartDate, returnDate,
                initialMileage, finalMileage, RentalStatus.COMPLETED);
    }
}
package com.rentamovil.booking.infrastructure.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.rentamovil.booking.domain.entity.Reservation;
import com.rentamovil.booking.domain.enums.ReservationStatus;

import lombok.Getter;

/** Respuesta de una reserva. clientId se expone para que admin pueda mostrar el titular. */
@Getter
public class ReservationResponse {

    private final Integer id;
    private final Integer clientId;
    private final Integer vehicleId;
    private final Integer insuranceTypeId;
    private final Integer pickupBranchId;
    private final Integer returnBranchId;
    private final LocalDateTime reservationDate;
    private final LocalDateTime startDate;
    private final LocalDateTime endDate;
    private final BigDecimal vehicleSubtotal;
    private final BigDecimal insuranceSubtotal;
    private final BigDecimal totalAmount;
    private final ReservationStatus status;

    private ReservationResponse(Integer id, Integer clientId, Integer vehicleId, Integer insuranceTypeId,
                                Integer pickupBranchId, Integer returnBranchId,
                                LocalDateTime reservationDate, LocalDateTime startDate,
                                LocalDateTime endDate, BigDecimal vehicleSubtotal,
                                BigDecimal insuranceSubtotal, BigDecimal totalAmount,
                                ReservationStatus status) {
        this.id = id;
        this.clientId = clientId;
        this.vehicleId = vehicleId;
        this.insuranceTypeId = insuranceTypeId;
        this.pickupBranchId = pickupBranchId;
        this.returnBranchId = returnBranchId;
        this.reservationDate = reservationDate;
        this.startDate = startDate;
        this.endDate = endDate;
        this.vehicleSubtotal = vehicleSubtotal;
        this.insuranceSubtotal = insuranceSubtotal;
        this.totalAmount = totalAmount;
        this.status = status;
    }

    public static ReservationResponse from(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getClientId(),
                reservation.getVehicleId(),
                reservation.getInsuranceTypeId(),
                reservation.getPickupBranchId(),
                reservation.getReturnBranchId(),
                reservation.getReservationDate(),
                reservation.getStartDate(),
                reservation.getEndDate(),
                reservation.getVehicleSubtotal(),
                reservation.getInsuranceSubtotal(),
                reservation.getTotalAmount(),
                reservation.getStatus());
    }
}

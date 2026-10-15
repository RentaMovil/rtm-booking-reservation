package com.rentamovil.booking.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Mapeo de booking.reservation. La tabla la crea rtm-booking-reservation-db; Hibernate solo valida. */
@Entity
@Table(name = "reservation")
@Getter
@Setter
@NoArgsConstructor
public class ReservationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reservation_id")
    private Integer id;

    /** FK a iam.person (rtm-iam-db). Sin relaciÃ³n JPA: person es de otro servicio. */
    @Column(name = "client_id", nullable = false)
    private Integer clientId;

    /** Sin FK todavÃ­a: fleet.vehicle no existe (ver rtm-booking-reservation-db/README). */
    @Column(name = "vehicle_id", nullable = false)
    private Integer vehicleId;

    @Column(name = "insurance_type_id")
    private Integer insuranceTypeId;

    @Column(name = "pickup_branch_id", nullable = false)
    private Integer pickupBranchId;

    @Column(name = "return_branch_id", nullable = false)
    private Integer returnBranchId;

    @Column(name = "reservation_date", nullable = false)
    private LocalDateTime reservationDate;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(name = "vehicle_subtotal", nullable = false, precision = 10, scale = 2)
    private BigDecimal vehicleSubtotal;

    @Column(name = "insurance_subtotal", nullable = false, precision = 10, scale = 2)
    private BigDecimal insuranceSubtotal;

    /** INV-008: no se escribe nunca; lo calcula el dominio como la suma de los dos subtotales. */
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    // String, no el enum: el estado se guarda y se lee como texto (el CHECK de la tabla lo valida
    // igual). El mapper convierte con getStatus().name() y ReservationStatus.valueOf().
    @Column(name = "status", nullable = false, length = 30)
    private String status;
}
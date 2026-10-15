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

/** Mapeo de booking.rental. */
@Entity
@Table(name = "rental")
@Getter
@Setter
@NoArgsConstructor
public class RentalEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rental_id")
    private Integer id;

    @Column(name = "reservation_id", nullable = false)
    private Integer reservationId;

    /** Sin FK todavÃ­a: telemetry.gps no existe. El Administrador lo indica en el pickup. */
    @Column(name = "gps_id", nullable = false)
    private Integer gpsId;

    @Column(name = "actual_start_date", nullable = false)
    private LocalDateTime actualStartDate;

    /** NULL mientras status = IN_PROGRESS. */
    @Column(name = "actual_end_date")
    private LocalDateTime actualEndDate;

    @Column(name = "initial_mileage", nullable = false, precision = 10, scale = 2)
    private BigDecimal initialMileage;

    @Column(name = "final_mileage", precision = 10, scale = 2)
    private BigDecimal finalMileage;

    // String, no el enum: igual que en ReservationEntity, el mapper hace la conversiÃ³n.
    @Column(name = "status", nullable = false, length = 30)
    private String status;
}
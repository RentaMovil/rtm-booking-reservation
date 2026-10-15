package com.rentamovil.booking.infrastructure.persistence.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.rentamovil.booking.infrastructure.persistence.entity.ReservationEntity;

/** Acceso Spring Data a booking.reservation. */
public interface ReservationJpaRepository extends JpaRepository<ReservationEntity, Integer> {

    List<ReservationEntity> findByClientIdOrderByReservationDateDesc(Integer clientId);

    List<ReservationEntity> findAllByOrderByReservationDateDesc();

    List<ReservationEntity> findByStatusOrderByReservationDateAsc(String status);

    Optional<ReservationEntity> findByIdAndClientId(Integer id, Integer clientId);

    /**
     * INV-015: reservas que siguen en PENDING_PAYMENT (nunca subieron comprobante) y cuya
     * reservation_date es más antigua que el corte. El job programado las expira a CANCELLED.
     * PENDING_REVIEW no expira: el comprobante ya se subió y lo decide el Administrador.
     */
    @Query("SELECT r FROM ReservationEntity r "
            + "WHERE r.status = 'PENDING_PAYMENT' "
            + "AND r.reservationDate < :cutoff")
    List<ReservationEntity> findExpiredUnpaid(@Param("cutoff") LocalDateTime cutoff);

    /**
     * Rangos que un vehículo tiene ocupados y que se solapan con [from, to].
     *
     * <p>Dos reservas se solapan si empiezan antes de que termine el rango consultado y terminan
     * después de que empiece: {@code start < :to AND end > :from}. CANCELLED no cuenta — un
     * vehículo cancelado queda libre de inmediato (INV-005, sin penalización).
     */
    @Query("SELECT r FROM ReservationEntity r "
            + "WHERE r.vehicleId = :vehicleId "
            + "AND r.status <> 'CANCELLED' "
            + "AND r.startDate < :to AND r.endDate > :from "
            + "ORDER BY r.startDate")
    List<ReservationEntity> findOccupiedRanges(@Param("vehicleId") Integer vehicleId,
                                               @Param("from") LocalDateTime from,
                                               @Param("to") LocalDateTime to);
}
package com.rentamovil.booking.domain.port.out;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.rentamovil.booking.domain.entity.Reservation;
import com.rentamovil.booking.domain.enums.ReservationStatus;

/** Lo que el dominio necesita del almacenamiento de reservas. */
public interface ReservationRepositoryPort {

    Reservation save(Reservation reservation);

    Optional<Reservation> findById(Integer id);

    /** HU-BOOKING-004: las reservas cuyo client_id es esta persona. */
    List<Reservation> findByClientId(Integer clientId);

    /** HU-BOOKING-008: todas las reservas, con filtro opcional por estado. */
    List<Reservation> findAll(ReservationStatus statusFilter);

    /**
     * Rango ocupado de un vehículo: las reservas no terminalmente cerradas que se solapan con el
     * rango pedido. Es lo que responde {@code GET /internal/reservations/vehicle-occupancy},
     * que consume fleet-maintenance.
     */
    List<OccupiedRange> findOccupiedRangesByVehicle(Integer vehicleId, LocalDateTime from,
                                                    LocalDateTime to);

    /**
     * INV-015: reservas PENDING_PAYMENT (el cliente nunca subió el comprobante) cuya
     * reservation_date es más antigua que el corte. Lo usa el job programado que las expira a
     * CANCELLED. PENDING_REVIEW no se incluye: ya tiene comprobante y lo decide el Administrador.
     */
    List<Reservation> findExpiredUnpaid(LocalDateTime cutoff);

    /** Un rango de fechas reservado para un vehículo. CANCELLED no ocupa: se liberó. */
    record OccupiedRange(Integer reservationId, LocalDateTime startDate, LocalDateTime endDate) {
    }
}
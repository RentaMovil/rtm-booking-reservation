package com.rentamovil.booking.support;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.rentamovil.booking.domain.entity.Reservation;
import com.rentamovil.booking.domain.enums.ReservationStatus;
import com.rentamovil.booking.domain.port.out.ReservationRepositoryPort;

/** Repositorio de reservas en memoria: prueba los casos de uso sin base de datos. */
public class InMemoryReservationRepository implements ReservationRepositoryPort {

    private final Map<Integer, Reservation> rows = new LinkedHashMap<>();
    private int nextId = 1;
    private int saveCount = 0;

    /** Carga un dato de prueba sin contarlo como escritura del caso de uso. */
    public Reservation put(Reservation reservation) {
        rows.put(reservation.getId(), reservation);
        nextId = Math.max(nextId, reservation.getId() + 1);
        return reservation;
    }

    /** Cuántas veces escribió el código bajo prueba. */
    public int saveCount() {
        return saveCount;
    }

    @Override
    public Reservation save(Reservation reservation) {
        saveCount++;
        Reservation stored = reservation.getId() != null ? reservation
                : Reservation.restore(nextId, reservation.getClientId(), reservation.getVehicleId(),
                        reservation.getInsuranceTypeId(), reservation.getPickupBranchId(),
                        reservation.getReturnBranchId(), reservation.getReservationDate(),
                        reservation.getStartDate(), reservation.getEndDate(),
                        reservation.getVehicleSubtotal(), reservation.getInsuranceSubtotal(),
                        reservation.getStatus());
        return put(stored);
    }

    @Override
    public Optional<Reservation> findById(Integer id) {
        return Optional.ofNullable(rows.get(id));
    }

    @Override
    public List<Reservation> findByClientId(Integer clientId) {
        return rows.values().stream().filter(r -> r.getClientId().equals(clientId)).toList();
    }

    @Override
    public List<Reservation> findAll(ReservationStatus statusFilter) {
        return rows.values().stream()
                .filter(r -> statusFilter == null || r.getStatus() == statusFilter)
                .toList();
    }

    @Override
    public List<OccupiedRange> findOccupiedRangesByVehicle(Integer vehicleId, LocalDateTime from,
                                                           LocalDateTime to) {
        return rows.values().stream()
                .filter(r -> r.getVehicleId().equals(vehicleId)
                        && r.getStatus() != ReservationStatus.CANCELLED
                        && r.getStartDate().isBefore(to) && r.getEndDate().isAfter(from))
                .map(r -> new OccupiedRange(r.getId(), r.getStartDate(), r.getEndDate()))
                .toList();
    }

    /**
     * A propósito más amplio que la consulta real (que solo trae PENDING_PAYMENT): devuelve toda
     * reserva anterior al corte, en cualquier estado. Así los tests prueban que la regla de
     * expiración no depende solo del SQL.
     */
    @Override
    public List<Reservation> findExpiredUnpaid(LocalDateTime cutoff) {
        return rows.values().stream().filter(r -> r.getReservationDate().isBefore(cutoff)).toList();
    }
}

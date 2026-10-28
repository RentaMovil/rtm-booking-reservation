package com.rentamovil.booking.domain.port.in;

import java.time.LocalDateTime;
import java.util.List;

import com.rentamovil.booking.domain.entity.Reservation;
import com.rentamovil.booking.domain.enums.ReservationStatus;
import com.rentamovil.booking.domain.port.out.ReservationRepositoryPort.OccupiedRange;

/** Casos de uso de reservas: HU-BOOKING-001..008. */
public interface ReservationUseCases {

    /**
     * HU-BOOKING-003: crea una reserva en PENDING_PAYMENT.
     * INV-010: que la sucursal de devolución sea distinta de la de recogida no lleva recargo.
     */
    Reservation create(Integer clientId, Integer vehicleId, Integer insuranceTypeId,
                       Integer pickupBranchId, Integer returnBranchId,
                       LocalDateTime startDate, LocalDateTime endDate, boolean termsAccepted);

    /** HU-BOOKING-004: las reservas propias del cliente autenticado. */
    List<Reservation> findMine(Integer clientId);

    /** HU-BOOKING-008: todas las reservas, con filtro opcional por estado. */
    List<Reservation> findAll(ReservationStatus statusFilter);

    Reservation findById(Integer id);

    /**
     * HU-BOOKING-005: cambiar la sucursal de devolución, lo único modificable de una reserva.
     * INV-013: hasta 3 días antes de end_date.
     */
    Reservation changeReturnBranch(Integer id, Integer clientId, Integer newReturnBranchId);

    /** Rango ocupado de un vehículo. Responde a fleet-maintenance, sin JWT. */
    List<OccupiedRange> vehicleOccupancy(Integer vehicleId, LocalDateTime from, LocalDateTime to);
}
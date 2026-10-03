package com.rentamovil.booking.infrastructure.web.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Getter;

/**
 * Respuesta de GET /internal/reservations/vehicle-occupancy.
 *
 * <p>La consume fleet-maintenance, no una persona. Las fechas van en ISO-8601 sin zona porque el
 * servicio entero trabaja en UTC (BookingReservationApplication).
 */
@Getter
public class VehicleOccupancyResponse {

    private final Integer vehicleId;
    private final boolean available;
    private final List<OccupiedRange> ranges;

    public VehicleOccupancyResponse(Integer vehicleId, boolean available, List<OccupiedRange> ranges) {
        this.vehicleId = vehicleId;
        this.available = available;
        this.ranges = ranges;
    }

    @Getter
    public static class OccupiedRange {
        private final Integer reservationId;
        private final LocalDateTime from;
        private final LocalDateTime to;

        public OccupiedRange(Integer reservationId, LocalDateTime from, LocalDateTime to) {
            this.reservationId = reservationId;
            this.from = from;
            this.to = to;
        }
    }
}
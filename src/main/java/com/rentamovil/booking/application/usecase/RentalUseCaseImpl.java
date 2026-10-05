package com.rentamovil.booking.application.usecase;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.rentamovil.booking.domain.entity.Rental;
import com.rentamovil.booking.domain.entity.Reservation;
import com.rentamovil.booking.domain.enums.ReservationStatus;
import com.rentamovil.booking.domain.exception.BusinessRuleException;
import com.rentamovil.booking.domain.exception.ResourceNotFoundException;
import com.rentamovil.booking.domain.port.in.RentalUseCases;
import com.rentamovil.booking.domain.port.out.AuditPort;
import com.rentamovil.booking.domain.port.out.DomainEventPublisherPort;
import com.rentamovil.booking.domain.port.out.RentalRepositoryPort;
import com.rentamovil.booking.domain.port.out.ReservationRepositoryPort;

/** HU-RENTAL-001 y HU-RENTAL-002: el pickup y la devolución. */
public class RentalUseCaseImpl implements RentalUseCases {

    private final RentalRepositoryPort rentalRepository;
    private final ReservationRepositoryPort reservationRepository;
    private final DomainEventPublisherPort events;
    private final AuditPort audit;

    public RentalUseCaseImpl(RentalRepositoryPort rentalRepository,
                             ReservationRepositoryPort reservationRepository,
                             DomainEventPublisherPort events,
                             AuditPort audit) {
        this.rentalRepository = rentalRepository;
        this.reservationRepository = reservationRepository;
        this.events = events;
        this.audit = audit;
    }

    @Override
    public Rental startPickup(Integer reservationId, Integer gpsId, LocalDateTime pickupDate,
                              BigDecimal initialMileage, Integer actorUserId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("La reserva no existe"));

        // Solo una reserva confirmada se puede recoger: sin pago aprobado el vehículo no sale.
        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new BusinessRuleException("RESERVATION_NOT_CONFIRMED",
                    "Solo se puede recoger el vehículo de una reserva confirmada");
        }

        // Una reserva se ejecuta exactamente una vez (UNIQUE en rental.reservation_id).
        if (rentalRepository.findByReservationId(reservationId).isPresent()) {
            throw new BusinessRuleException("RENTAL_ALREADY_STARTED",
                    "Esta reserva ya tiene una renta registrada");
        }

        Rental rental = Rental.startPickup(reservationId, reservation.getVehicleId(), gpsId,
                pickupDate, initialMileage);
        Rental saved = rentalRepository.save(rental);

        // RentalStarted: fleet-maintenance pone vehicle.status = RENTED y telemetry-gps empieza a
        // esperar reportes de ese gpsId. Es el punto donde NO se escribe vehicle.status directo.
        events.publish("RentalStarted", Map.of(
                "rentalId", saved.getId(),
                "reservationId", reservationId,
                "vehicleId", reservation.getVehicleId(),
                "gpsId", gpsId));

        audit.record(actorUserId, "RENTAL_STARTED", "rental", saved.getId(), null);
        return saved;
    }

    @Override
    public Rental completeReturn(Integer rentalId, LocalDateTime returnDate, BigDecimal finalMileage,
                                 Integer actorUserId) {
        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new ResourceNotFoundException("La renta no existe"));

        Rental completed = rental.completeReturn(returnDate, finalMileage);
        Rental saved = rentalRepository.save(completed);

        // RentalCompleted: fleet-maintenance pone vehicle.status = AVAILABLE, telemetry-gps deja de
        // esperar reportes, y la reserva pasa a COMPLETED — el ciclo entero cierra aquí.
        reservationRepository.findById(rental.getReservationId()).ifPresent(reservation -> {
            Reservation closed = reservationRepository
                    .save(reservation.transitionTo(ReservationStatus.COMPLETED));
            // vehicleId sale de la reserva, no de rental: la tabla rental no tiene esa columna.
            events.publish("RentalCompleted", Map.of(
                    "rentalId", saved.getId(),
                    "reservationId", closed.getId(),
                    "vehicleId", closed.getVehicleId(),
                    "gpsId", saved.getGpsId()));
        });

        audit.record(actorUserId, "RENTAL_COMPLETED", "rental", saved.getId(), null);
        return saved;
    }

    @Override
    public Rental findById(Integer id) {
        return rentalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("La renta no existe"));
    }

    @Override
    public List<Rental> findInProgress() {
        return rentalRepository.findInProgress();
    }

    @Override
    public List<Rental> findAll() {
        return rentalRepository.findAll();
    }
}
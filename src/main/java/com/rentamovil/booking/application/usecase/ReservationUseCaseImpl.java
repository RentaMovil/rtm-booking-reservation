package com.rentamovil.booking.application.usecase;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.rentamovil.booking.domain.entity.InsuranceType;
import com.rentamovil.booking.domain.entity.Notification;
import com.rentamovil.booking.domain.entity.Reservation;
import com.rentamovil.booking.domain.enums.NotificationType;
import com.rentamovil.booking.domain.enums.ReservationStatus;
import com.rentamovil.booking.domain.exception.BusinessRuleException;
import com.rentamovil.booking.domain.exception.ResourceNotFoundException;
import com.rentamovil.booking.domain.port.in.ReservationUseCases;
import com.rentamovil.booking.domain.port.out.AuditPort;
import com.rentamovil.booking.domain.port.out.DomainEventPublisherPort;
import com.rentamovil.booking.domain.port.out.InsuranceTypeRepositoryPort;
import com.rentamovil.booking.domain.port.out.NotificationRepositoryPort;
import com.rentamovil.booking.domain.port.out.ReservationRepositoryPort;
import com.rentamovil.booking.domain.port.out.ReservationRepositoryPort.OccupiedRange;
import com.rentamovil.booking.domain.port.out.VehicleAvailabilityPort;

/** HU-BOOKING-003, 004, 005, 006 y 008. */
public class ReservationUseCaseImpl implements ReservationUseCases {

    /** INV-004: cancelar solo hasta 3 días antes de la fecha de recogida. */
    private static final long CANCELLATION_DEADLINE_DAYS = 3;

    private final ReservationRepositoryPort reservationRepository;
    private final InsuranceTypeRepositoryPort insuranceTypeRepository;
    private final NotificationRepositoryPort notificationRepository;
    private final VehicleAvailabilityPort vehicleAvailability;
    private final DomainEventPublisherPort events;
    private final AuditPort audit;

    public ReservationUseCaseImpl(ReservationRepositoryPort reservationRepository,
                                   InsuranceTypeRepositoryPort insuranceTypeRepository,
                                   NotificationRepositoryPort notificationRepository,
                                   VehicleAvailabilityPort vehicleAvailability,
                                   DomainEventPublisherPort events,
                                   AuditPort audit) {
        this.reservationRepository = reservationRepository;
        this.insuranceTypeRepository = insuranceTypeRepository;
        this.notificationRepository = notificationRepository;
        this.vehicleAvailability = vehicleAvailability;
        this.events = events;
        this.audit = audit;
    }

    @Override
    public Reservation create(Integer clientId, Integer vehicleId, Integer insuranceTypeId,
                              Integer pickupBranchId, Integer returnBranchId,
                              LocalDateTime startDate, LocalDateTime endDate, boolean termsAccepted) {
        // INV-012: crear una reserva exige sesión iniciada; el id del titular viene del token.
        if (clientId == null) {
            throw new BusinessRuleException("AUTHENTICATION_REQUIRED", "Debes iniciar sesión para reservar");
        }
        // HU-BOOKING-007: los términos y condiciones son parte del wizard de creación.
        if (!termsAccepted) {
            throw new BusinessRuleException("TERMS_NOT_ACCEPTED",
                    "Debes aceptar los términos y condiciones para reservar");
        }

        long days = daysBetween(startDate, endDate);

        if (!vehicleAvailability.isAvailable(vehicleId, startDate, endDate)) {
            throw new BusinessRuleException("VEHICLE_NOT_AVAILABLE",
                    "El vehículo no está disponible para las fechas indicadas");
        }

        BigDecimal vehicleSubtotal = vehicleAvailability.dailyRate(vehicleId, startDate, endDate)
                .multiply(BigDecimal.valueOf(days))
                .setScale(2, java.math.RoundingMode.HALF_UP);

        // INV-006: como máximo un seguro. Si viene id, se carga para cotizarlo.
        BigDecimal insuranceSubtotal = BigDecimal.ZERO;
        if (insuranceTypeId != null) {
            InsuranceType plan = insuranceTypeRepository.findById(insuranceTypeId)
                    .orElseThrow(() -> new ResourceNotFoundException("El plan de seguro no existe"));
            // INV-007: se cobra una vez por la duración total de la reserva
            insuranceSubtotal = plan.costFor(days);
        }

        // INV-010: sucursal de devolución distinta de la de recogida no lleva recargo. Por eso el
        // total es vehicle_subtotal + insurance_subtotal, sin un tercer término por sucursal.
        Reservation reservation = Reservation.create(clientId, vehicleId, insuranceTypeId,
                pickupBranchId, returnBranchId, startDate, endDate, vehicleSubtotal, insuranceSubtotal);

        Reservation saved = reservationRepository.save(reservation);

        events.publish("ReservationCreated", Map.of(
                "reservationId", saved.getId(),
                "clientId", saved.getClientId(),
                "vehicleId", saved.getVehicleId(),
                "totalAmount", saved.getTotalAmount()));

        // El Administrador necesita enterarse de que hay una reserva nueva (HU-BOOKING-008).
        notificationRepository.save(Notification.aboutReservation(clientId,
                NotificationType.RESERVATION_CREATED,
                "Tu reserva #" + saved.getId() + " fue creada. Falta subir el comprobante de pago.",
                saved.getId()));

        audit.record(clientId, "RESERVATION_CREATED", "reservation", saved.getId(), null);
        return saved;
    }

    @Override
    public List<Reservation> findMine(Integer clientId) {
        // INV-011: puede tener varias reservas activas a la vez; no se filtra por estado.
        return reservationRepository.findByClientId(clientId);
    }

    @Override
    public List<Reservation> findAll(ReservationStatus statusFilter) {
        return reservationRepository.findAll(statusFilter);
    }

    @Override
    public Reservation findById(Integer id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("La reserva no existe"));
    }

    @Override
    public Reservation cancel(Integer id, Integer clientId) {
        Reservation reservation = findById(id);

        // No se cancela la reserva de otra persona, aunque se conozca su id.
        if (!reservation.getClientId().equals(clientId)) {
            throw new ResourceNotFoundException("La reserva no existe");
        }

        // INV-004: solo hasta 3 días antes de empezar.
        LocalDateTime deadline = reservation.getStartDate().minusDays(CANCELLATION_DEADLINE_DAYS);
        if (LocalDateTime.now().isAfter(deadline)) {
            throw new BusinessRuleException("CANCELLATION_WINDOW_CLOSED",
                    "Ya no se puede cancelar: el plazo límite era 3 días antes de la fecha de recogida");
        }

        // INV-005: cancelar no genera recargo. transitionTo valida que el estado actual lo permita.
        Reservation cancelled = reservationRepository
                .save(reservation.transitionTo(ReservationStatus.CANCELLED));

        events.publish("ReservationCancelled", Map.of(
                "reservationId", id,
                "clientId", clientId));

        notificationRepository.save(Notification.aboutReservation(clientId,
                NotificationType.RESERVATION_CANCELLED, "Tu reserva #" + id + " fue cancelada.", id));

        audit.record(clientId, "RESERVATION_CANCELLED", "reservation", id, null);
        return cancelled;
    }

    @Override
    public Reservation modify(Integer id, Integer clientId, LocalDateTime newEndDate,
                              Integer newReturnBranchId) {
        Reservation reservation = findById(id);

        if (!reservation.getClientId().equals(clientId)) {
            throw new ResourceNotFoundException("La reserva no existe");
        }

        boolean branchChanged = newReturnBranchId != null
                && !newReturnBranchId.equals(reservation.getReturnBranchId());

        Reservation modified = reservation;
        if (newEndDate != null) {
            // INV-003: solo ampliar, nunca acortar.
            modified = modified.extendEndDate(newEndDate);
        }
        if (branchChanged) {
            // INV-013: hasta 3 días antes de end_date.
            modified = modified.changeReturnBranch(newReturnBranchId, LocalDateTime.now());
        }
        if (newEndDate == null && !branchChanged) {
            return reservation;
        }

        Reservation saved = reservationRepository.save(modified);

        if (newEndDate != null) {
            events.publish("ReservationReturnDateExtended", Map.of(
                    "reservationId", id, "newEndDate", newEndDate.toString()));
            notificationRepository.save(Notification.aboutReservation(clientId,
                    NotificationType.RESERVATION_RETURN_DATE_EXTENDED,
                    "La fecha de devolución de tu reserva #" + id + " cambió.", id));
        }
        if (branchChanged) {
            events.publish("ReservationReturnBranchChanged", Map.of(
                    "reservationId", id, "newReturnBranchId", newReturnBranchId));
            notificationRepository.save(Notification.aboutReservation(clientId,
                    NotificationType.RESERVATION_RETURN_BRANCH_CHANGED,
                    "La sucursal de devolución de tu reserva #" + id + " cambió.", id));
        }

        audit.record(clientId, "RESERVATION_MODIFIED", "reservation", id, null);
        return saved;
    }

    @Override
    public List<OccupiedRange> vehicleOccupancy(Integer vehicleId, LocalDateTime from, LocalDateTime to) {
        // Sin JWT: lo consume fleet-maintenance, no una persona (README → "Dependencia síncrona").
        return reservationRepository.findOccupiedRangesByVehicle(vehicleId, from, to);
    }

    /** Un alquiler de menos de 24 h cuenta como un día mínimo: el seguro se cobra por día. */
    private static long daysBetween(LocalDateTime start, LocalDateTime end) {
        return Math.max(Duration.between(start, end).toDays(), 1);
    }
}
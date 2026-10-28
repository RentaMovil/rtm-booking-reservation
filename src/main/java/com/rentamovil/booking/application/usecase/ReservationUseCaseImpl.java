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
import com.rentamovil.booking.domain.port.out.RealtimeNotificationPort;
import com.rentamovil.booking.domain.port.out.ReservationRepositoryPort;
import com.rentamovil.booking.domain.port.out.ReservationRepositoryPort.OccupiedRange;
import com.rentamovil.booking.domain.port.out.VehicleAvailabilityPort;

/** HU-BOOKING-003, 004, 005 y 008. */
public class ReservationUseCaseImpl implements ReservationUseCases {

    private final ReservationRepositoryPort reservationRepository;
    private final InsuranceTypeRepositoryPort insuranceTypeRepository;
    private final NotificationRepositoryPort notificationRepository;
    private final RealtimeNotificationPort realtimeNotification;
    private final VehicleAvailabilityPort vehicleAvailability;
    private final DomainEventPublisherPort events;
    private final AuditPort audit;

    public ReservationUseCaseImpl(ReservationRepositoryPort reservationRepository,
                                   InsuranceTypeRepositoryPort insuranceTypeRepository,
                                   NotificationRepositoryPort notificationRepository,
                                   RealtimeNotificationPort realtimeNotification,
                                   VehicleAvailabilityPort vehicleAvailability,
                                   DomainEventPublisherPort events,
                                   AuditPort audit) {
        this.reservationRepository = reservationRepository;
        this.insuranceTypeRepository = insuranceTypeRepository;
        this.notificationRepository = notificationRepository;
        this.realtimeNotification = realtimeNotification;
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

        Notification clientNotification = notificationRepository.save(Notification.aboutReservation(clientId,
                NotificationType.RESERVATION_CREATED,
                "Tu reserva #" + saved.getId() + " fue creada. Falta subir el comprobante de pago.",
                saved.getId()));
        realtimeNotification.pushToUser(clientNotification);

        // El Administrador necesita enterarse de que hay una reserva nueva (HU-BOOKING-008).
        // Solo en vivo: no hay fila propia para "cualquier admin conectado" (ver RealtimeNotificationPort).
        realtimeNotification.pushToAdmins("RESERVATION_CREATED",
                "Nueva reserva #" + saved.getId() + " creada, pendiente de comprobante de pago.",
                saved.getId());

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
    public Reservation changeReturnBranch(Integer id, Integer clientId, Integer newReturnBranchId) {
        Reservation reservation = findById(id);

        // No se modifica la reserva de otra persona, aunque se conozca su id.
        if (!reservation.getClientId().equals(clientId)) {
            throw new ResourceNotFoundException("La reserva no existe");
        }

        // Misma sucursal: no hay nada que guardar, notificar ni auditar.
        if (newReturnBranchId.equals(reservation.getReturnBranchId())) {
            return reservation;
        }

        // Estado vigente e INV-013 (hasta 3 días antes de end_date): los valida el dominio.
        Reservation saved = reservationRepository.save(
                reservation.changeReturnBranch(newReturnBranchId, LocalDateTime.now()));

        events.publish("ReservationReturnBranchChanged", Map.of(
                "reservationId", id, "newReturnBranchId", newReturnBranchId));
        notificationRepository.save(Notification.aboutReservation(clientId,
                NotificationType.RESERVATION_RETURN_BRANCH_CHANGED,
                "La sucursal de devolución de tu reserva #" + id + " cambió.", id));

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
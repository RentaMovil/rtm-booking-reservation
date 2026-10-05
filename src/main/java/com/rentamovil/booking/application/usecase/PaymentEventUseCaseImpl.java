package com.rentamovil.booking.application.usecase;

import java.util.EnumSet;
import java.util.Set;

import com.rentamovil.booking.domain.entity.Notification;
import com.rentamovil.booking.domain.entity.Reservation;
import com.rentamovil.booking.domain.enums.NotificationType;
import com.rentamovil.booking.domain.enums.ReservationStatus;
import com.rentamovil.booking.domain.exception.BusinessRuleException;
import com.rentamovil.booking.domain.port.in.PaymentEventUseCases;
import com.rentamovil.booking.domain.port.out.NotificationRepositoryPort;
import com.rentamovil.booking.domain.port.out.ReservationRepositoryPort;

/**
 * Reacciones a los eventos de payment-billing y fleet-maintenance.
 *
 * <p>Idempotentes: la garantía de entrega es at-least-once. Si la reserva ya está en el estado al
 * que la llevaría el evento (o más adelante en el flujo), el evento se ignora: no se guarda nada
 * ni se vuelve a notificar. Así un evento repetido o atrasado no duplica la notificación ni
 * responde con error al servicio que lo reenvía.
 */
public class PaymentEventUseCaseImpl implements PaymentEventUseCases {

    /** INV-016: qué hacer con la reserva cuando el Administrador rechaza el comprobante. */
    private static final String OUTCOME_RETRY_ALLOWED = "RETRY_ALLOWED";
    private static final String OUTCOME_RESERVATION_CANCELLED = "RESERVATION_CANCELLED";

    /** Estados en los que el comprobante ya se subió (o el flujo de pago ya terminó). */
    private static final Set<ReservationStatus> ALREADY_SUBMITTED = EnumSet.of(
            ReservationStatus.PENDING_REVIEW, ReservationStatus.CONFIRMED, ReservationStatus.COMPLETED);

    /** Estados en los que el pago ya se aprobó. */
    private static final Set<ReservationStatus> ALREADY_CONFIRMED = EnumSet.of(
            ReservationStatus.CONFIRMED, ReservationStatus.COMPLETED);

    private final ReservationRepositoryPort reservationRepository;
    private final NotificationRepositoryPort notificationRepository;

    public PaymentEventUseCaseImpl(ReservationRepositoryPort reservationRepository,
                                   NotificationRepositoryPort notificationRepository) {
        this.reservationRepository = reservationRepository;
        this.notificationRepository = notificationRepository;
    }

    @Override
    public boolean onPaymentSubmitted(Integer reservationId, Integer paymentId) {
        Reservation reservation = find(reservationId);
        if (ALREADY_SUBMITTED.contains(reservation.getStatus())) {
            return false;
        }

        // Solo desde PENDING_PAYMENT: transitionTo rechaza el resto (por ejemplo, una CANCELLED).
        // Sin notificación: el CHECK de booking.notification no tiene un tipo para este evento.
        reservationRepository.save(reservation.transitionTo(ReservationStatus.PENDING_REVIEW));
        return true;
    }

    @Override
    public boolean onPaymentCompleted(Integer reservationId, Integer paymentId, Integer actorUserId) {
        Reservation reservation = find(reservationId);
        if (ALREADY_CONFIRMED.contains(reservation.getStatus())) {
            return false;
        }

        // INV-009: el pago aprobado es lo que confirma la reserva. Exige PENDING_REVIEW.
        Reservation confirmed = reservationRepository
                .save(reservation.transitionTo(ReservationStatus.CONFIRMED));

        notify(confirmed, NotificationType.PAYMENT_APPROVED,
                "Tu pago de la reserva #" + reservationId + " fue aprobado. La reserva está confirmada.",
                paymentId, null);
        return true;
    }

    @Override
    public boolean onPaymentRejected(Integer reservationId, Integer paymentId, String outcome,
                                     String rejectionReason) {
        // El outcome se valida antes que nada: un valor desconocido es un error de quien envía.
        ReservationStatus target;
        if (OUTCOME_RESERVATION_CANCELLED.equals(outcome)) {
            target = ReservationStatus.CANCELLED;
        } else if (OUTCOME_RETRY_ALLOWED.equals(outcome)) {
            target = ReservationStatus.PENDING_PAYMENT;
        } else {
            throw new BusinessRuleException("UNKNOWN_PAYMENT_OUTCOME",
                    "outcome de rechazo desconocido: " + outcome);
        }

        Reservation reservation = find(reservationId);
        if (reservation.getStatus() == target) {
            return false;
        }

        Reservation updated = reservationRepository.save(reservation.transitionTo(target));
        String reason = rejectionReason == null ? "" : "Motivo: " + rejectionReason;
        String message = target == ReservationStatus.CANCELLED
                // INV-016: el Administrador se rindió con esta reserva.
                ? "Tu pago de la reserva #" + reservationId + " fue rechazado y la reserva fue cancelada. " + reason
                // El cliente puede volver a subir un comprobante.
                : "Tu pago de la reserva #" + reservationId + " fue rechazado. Puedes subir otro comprobante. " + reason;
        notify(updated, NotificationType.PAYMENT_REJECTED, message.trim(), paymentId, null);
        return true;
    }

    @Override
    public boolean onInvoiceGenerated(Integer reservationId, Integer invoiceId, Integer paymentId) {
        Reservation reservation = find(reservationId);
        if (notificationRepository.existsByInvoiceId(invoiceId)) {
            return false;
        }

        // InvoiceGenerated no tiene notification_type propio: se agrupa bajo PAYMENT_APPROVED
        // porque ambos ocurren en el mismo momento (events.md). Si alguna vez hay que
        // distinguirlos en la campanita, hace falta agregar un valor al CHECK de la tabla.
        notify(reservation, NotificationType.PAYMENT_APPROVED,
                "Tu factura de la reserva #" + reservationId + " ya está disponible.", paymentId, invoiceId);
        return true;
    }

    @Override
    public boolean onMaintenanceCompleted(Integer personId, Integer maintenanceId, String vehicleName) {
        if (notificationRepository.existsByMaintenanceIdAndPersonId(maintenanceId, personId)) {
            return false;
        }
        notificationRepository.save(Notification.aboutMaintenance(personId,
                NotificationType.MAINTENANCE_COMPLETED,
                "El mantenimiento de " + vehicleName + " terminó. Ya puedes volver a reservarlo.",
                maintenanceId));
        return true;
    }

    private Reservation find(Integer reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new BusinessRuleException("RESERVATION_NOT_FOUND",
                        "La reserva no existe"));
    }

    private void notify(Reservation reservation, NotificationType type, String message,
                        Integer paymentId, Integer invoiceId) {
        notificationRepository.save(Notification.aboutPayment(reservation.getClientId(), type, message,
                reservation.getId(), paymentId, invoiceId));
    }
}

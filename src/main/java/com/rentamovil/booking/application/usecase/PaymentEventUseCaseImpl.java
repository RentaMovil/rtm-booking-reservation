package com.rentamovil.booking.application.usecase;

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
 * <p>Todos los métodos son idempotentes: la garantía de entrega es at-least-once y el mismo
 * evento puede llegar dos veces. La transición de estado lo es por diseño
 * ({@code Reservation.transitionTo} devuelve la misma entidad si ya está en el estado destino).
 */
public class PaymentEventUseCaseImpl implements PaymentEventUseCases {

    /** INV-016: qué hacer con la reserva cuando el Administrador rechaza el comprobante. */
    private static final String OUTCOME_RETRY_ALLOWED = "RETRY_ALLOWED";
    private static final String OUTCOME_RESERVATION_CANCELLED = "RESERVATION_CANCELLED";

    private final ReservationRepositoryPort reservationRepository;
    private final NotificationRepositoryPort notificationRepository;

    public PaymentEventUseCaseImpl(ReservationRepositoryPort reservationRepository,
                                   NotificationRepositoryPort notificationRepository) {
        this.reservationRepository = reservationRepository;
        this.notificationRepository = notificationRepository;
    }

    @Override
    public void onPaymentCompleted(Integer reservationId, Integer paymentId, Integer actorUserId) {
        Reservation reservation = find(reservationId);

        // INV-009: el pago aprobado es lo que confirma la reserva.
        Reservation confirmed = reservationRepository
                .save(reservation.transitionTo(ReservationStatus.CONFIRMED));

        notify(confirmed, NotificationType.PAYMENT_APPROVED,
                "Tu pago de la reserva #" + reservationId + " fue aprobado. La reserva está confirmada.",
                paymentId, null);
    }

    @Override
    public void onPaymentRejected(Integer reservationId, Integer paymentId, String outcome,
                                  String rejectionReason) {
        Reservation reservation = find(reservationId);

        if (OUTCOME_RESERVATION_CANCELLED.equals(outcome)) {
            // INV-016: el Administrador se rindió con esta reserva.
            Reservation cancelled = reservationRepository
                    .save(reservation.transitionTo(ReservationStatus.CANCELLED));
            notify(cancelled, NotificationType.PAYMENT_REJECTED,
                    "Tu pago de la reserva #" + reservationId + " fue rechazado y la reserva fue cancelada. "
                            + (rejectionReason == null ? "" : "Motivo: " + rejectionReason),
                    paymentId, null);
            return;
        }

        if (!OUTCOME_RETRY_ALLOWED.equals(outcome)) {
            throw new BusinessRuleException("UNKNOWN_PAYMENT_OUTCOME",
                    "outcome de rechazo desconocido: " + outcome);
        }

        // El cliente puede volver a subir un comprobante.
        Reservation back = reservationRepository
                .save(reservation.transitionTo(ReservationStatus.PENDING_PAYMENT));
        notify(back, NotificationType.PAYMENT_REJECTED,
                "Tu pago de la reserva #" + reservationId + " fue rechazado. Puedes subir otro comprobante. "
                        + (rejectionReason == null ? "" : "Motivo: " + rejectionReason),
                paymentId, null);
    }

    @Override
    public void onInvoiceGenerated(Integer reservationId, Integer invoiceId, Integer paymentId) {
        Reservation reservation = find(reservationId);

        // InvoiceGenerated no tiene notification_type propio: se agrupa bajo PAYMENT_APPROVED
        // porque ambos ocurren en el mismo momento (events.md). Si alguna vez hay que
        // distinguirlos en la campanita, hace falta agregar un valor al CHECK de la tabla.
        notify(reservation, NotificationType.PAYMENT_APPROVED,
                "Tu factura de la reserva #" + reservationId + " ya está disponible.", paymentId, invoiceId);
    }

    @Override
    public void onMaintenanceCompleted(Integer personId, Integer maintenanceId, String vehicleName) {
        notificationRepository.save(Notification.aboutMaintenance(personId,
                NotificationType.MAINTENANCE_COMPLETED,
                "El mantenimiento de " + vehicleName + " terminó. Ya puedes volver a reservarlo.",
                maintenanceId));
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
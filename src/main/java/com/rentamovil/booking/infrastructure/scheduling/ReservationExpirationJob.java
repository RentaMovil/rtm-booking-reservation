package com.rentamovil.booking.infrastructure.scheduling;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.rentamovil.booking.domain.entity.Notification;
import com.rentamovil.booking.domain.entity.Reservation;
import com.rentamovil.booking.domain.enums.NotificationType;
import com.rentamovil.booking.domain.enums.ReservationStatus;
import com.rentamovil.booking.domain.port.out.AuditPort;
import com.rentamovil.booking.domain.port.out.DomainEventPublisherPort;
import com.rentamovil.booking.domain.port.out.NotificationRepositoryPort;
import com.rentamovil.booking.domain.port.out.ReservationRepositoryPort;

/**
 * INV-015: una reserva sin pago aprobado 24 h después de creada se cancela sola.
 *
 * <p>Es la primera transición del sistema que no dispara un Administrador (events.md), por eso
 * se escribe en audit con user_id NULL — "acción de sistema", no de una persona.
 */
@Component
public class ReservationExpirationJob {

    private static final Logger log = LoggerFactory.getLogger(ReservationExpirationJob.class);

    private final ReservationRepositoryPort reservationRepository;
    private final NotificationRepositoryPort notificationRepository;
    private final DomainEventPublisherPort events;
    private final AuditPort audit;
    private final long windowHours;

    public ReservationExpirationJob(ReservationRepositoryPort reservationRepository,
                                    NotificationRepositoryPort notificationRepository,
                                    DomainEventPublisherPort events,
                                    AuditPort audit,
                                    @Value("${app.booking-reservation.reservation.payment-window-hours:24}")
                                    long windowHours) {
        this.reservationRepository = reservationRepository;
        this.notificationRepository = notificationRepository;
        this.events = events;
        this.audit = audit;
        this.windowHours = windowHours;
    }

    /** Corre cada hora: una reserva no necesita expirar al minuto, con que pase dentro de la hora. */
    @Scheduled(cron = "0 0 * * * *")
    public void expireUnpaidReservations() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(windowHours);

        List<Reservation> expired = reservationRepository.findExpiredUnpaid(cutoff);
        if (expired.isEmpty()) {
            return;
        }

        for (Reservation reservation : expired) {
            try {
                Reservation cancelled = reservationRepository
                        .save(reservation.transitionTo(ReservationStatus.CANCELLED));

                events.publish("ReservationExpired", java.util.Map.of(
                        "reservationId", cancelled.getId(),
                        "clientId", cancelled.getClientId()));

                notificationRepository.save(Notification.aboutReservation(
                        cancelled.getClientId(), NotificationType.RESERVATION_EXPIRED,
                        "Tu reserva #" + cancelled.getId() + " expiró: no recibimos el pago a tiempo.",
                        cancelled.getId()));

                // user_id NULL: no hay actor humano detrás de esta cancelación.
                audit.recordSystem("RESERVATION_EXPIRED", "reservation", cancelled.getId(), null);
            } catch (Exception e) {
                // Una reserva que falla no debe impedir que las demás expiren.
                log.error("No se pudo expirar la reserva {}", reservation.getId(), e);
            }
        }

        log.info("Se expiraron {} reservas sin pago aprobado", expired.size());
    }
}
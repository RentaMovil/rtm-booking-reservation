package com.rentamovil.booking.infrastructure.scheduling;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.rentamovil.booking.domain.entity.Reservation;
import com.rentamovil.booking.domain.enums.NotificationType;
import com.rentamovil.booking.domain.enums.ReservationStatus;
import com.rentamovil.booking.domain.port.out.AuditPort;
import com.rentamovil.booking.domain.port.out.DomainEventPublisherPort;
import com.rentamovil.booking.support.InMemoryNotificationRepository;
import com.rentamovil.booking.support.InMemoryReservationRepository;

/** INV-015: only reservations that never uploaded a receipt expire. */
class ReservationExpirationJobTest {

    private final InMemoryReservationRepository reservations = new InMemoryReservationRepository();
    private final InMemoryNotificationRepository notifications = new InMemoryNotificationRepository();
    private final List<String> publishedEvents = new ArrayList<>();
    private final List<String> systemAudits = new ArrayList<>();

    private final DomainEventPublisherPort events = new DomainEventPublisherPort() {
        @Override
        public void publish(String eventType, Object payload) {
            publishedEvents.add(eventType);
        }

        @Override
        public void subscribe(String eventType, EventListener listener) {
        }
    };

    private final AuditPort audit = new AuditPort() {
        @Override
        public void record(Integer userId, String action, String affectedTable, Integer affectedRecordId,
                           String detailJson) {
        }

        @Override
        public void recordSystem(String action, String affectedTable, Integer affectedRecordId,
                                 String detailJson) {
            systemAudits.add(action);
        }
    };

    private final ReservationExpirationJob job =
            new ReservationExpirationJob(reservations, notifications, events, audit, 24);

    private void givenReservation(Integer id, ReservationStatus status, int hoursAgo) {
        LocalDateTime start = LocalDateTime.now().plusDays(10);
        reservations.put(Reservation.restore(id, 7, 301, null, 1, 2,
                LocalDateTime.now().minusHours(hoursAgo), start, start.plusDays(4),
                new BigDecimal("400000.00"), BigDecimal.ZERO, status));
    }

    private ReservationStatus statusOf(Integer id) {
        return reservations.findById(id).orElseThrow().getStatus();
    }

    @Test
    @DisplayName("A PENDING_PAYMENT reservation older than 24h is cancelled, notified and audited")
    void expiresTheOneThatNeverUploadedAReceipt() {
        givenReservation(1, ReservationStatus.PENDING_PAYMENT, 30);

        job.expireUnpaidReservations();

        assertThat(statusOf(1)).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(notifications.all()).singleElement()
                .satisfies(n -> assertThat(n.getType()).isEqualTo(NotificationType.RESERVATION_EXPIRED));
        assertThat(publishedEvents).containsExactly("ReservationExpired");
        assertThat(systemAudits).containsExactly("RESERVATION_EXPIRED");
    }

    @Test
    @DisplayName("A PENDING_REVIEW reservation doesn't expire even past 24h: the receipt was already uploaded")
    void doesNotExpireTheOneUnderReview() {
        givenReservation(1, ReservationStatus.PENDING_REVIEW, 30);

        job.expireUnpaidReservations();

        assertThat(statusOf(1)).isEqualTo(ReservationStatus.PENDING_REVIEW);
        assertThat(notifications.all()).isEmpty();
        assertThat(publishedEvents).isEmpty();
        assertThat(reservations.saveCount()).isZero();
    }

    @Test
    @DisplayName("With several reservations, only the overdue PENDING_PAYMENT one expires")
    void onlyExpiresTheRightOne() {
        givenReservation(1, ReservationStatus.PENDING_PAYMENT, 30);
        givenReservation(2, ReservationStatus.PENDING_REVIEW, 30);
        givenReservation(3, ReservationStatus.PENDING_PAYMENT, 2);
        givenReservation(4, ReservationStatus.CONFIRMED, 30);

        job.expireUnpaidReservations();

        assertThat(statusOf(1)).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(statusOf(2)).isEqualTo(ReservationStatus.PENDING_REVIEW);
        assertThat(statusOf(3)).isEqualTo(ReservationStatus.PENDING_PAYMENT);
        assertThat(statusOf(4)).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(notifications.all()).hasSize(1);
    }
}

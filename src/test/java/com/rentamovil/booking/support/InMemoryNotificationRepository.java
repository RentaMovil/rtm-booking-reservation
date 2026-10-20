package com.rentamovil.booking.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.rentamovil.booking.domain.entity.Notification;
import com.rentamovil.booking.domain.port.out.NotificationRepositoryPort;

/** In-memory notification repository. */
public class InMemoryNotificationRepository implements NotificationRepositoryPort {

    private final List<Notification> rows = new ArrayList<>();

    /** Every notification saved, in order. */
    public List<Notification> all() {
        return List.copyOf(rows);
    }

    @Override
    public Notification save(Notification n) {
        Notification stored = Notification.restore(rows.size() + 1, n.getPersonId(), n.getType(),
                n.getMessage(), n.getSentDate(), n.isRead(), n.getReservationId(), n.getInvoiceId(),
                n.getPaymentId(), n.getMaintenanceId());
        rows.add(stored);
        return stored;
    }

    @Override
    public Optional<Notification> findById(Integer id) {
        return rows.stream().filter(n -> n.getId().equals(id)).findFirst();
    }

    @Override
    public List<Notification> findByPersonId(Integer personId) {
        return rows.stream().filter(n -> n.getPersonId().equals(personId)).toList();
    }

    @Override
    public long countUnread(Integer personId) {
        return rows.stream().filter(n -> n.getPersonId().equals(personId) && !n.isRead()).count();
    }

    @Override
    public boolean existsByInvoiceId(Integer invoiceId) {
        return rows.stream().anyMatch(n -> Objects.equals(n.getInvoiceId(), invoiceId));
    }

    @Override
    public boolean existsByMaintenanceIdAndPersonId(Integer maintenanceId, Integer personId) {
        return rows.stream().anyMatch(n -> Objects.equals(n.getMaintenanceId(), maintenanceId)
                && Objects.equals(n.getPersonId(), personId));
    }
}

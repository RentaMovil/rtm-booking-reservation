package com.rentamovil.booking.application.usecase;

import java.util.List;

import com.rentamovil.booking.domain.entity.Notification;
import com.rentamovil.booking.domain.exception.ResourceNotFoundException;
import com.rentamovil.booking.domain.port.in.NotificationUseCases;
import com.rentamovil.booking.domain.port.out.NotificationRepositoryPort;

/** HU-NOTIF-001: la campanita de notificaciones. */
public class NotificationUseCaseImpl implements NotificationUseCases {

    private final NotificationRepositoryPort notificationRepository;

    public NotificationUseCaseImpl(NotificationRepositoryPort notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public List<Notification> findMine(Integer personId) {
        return notificationRepository.findByPersonId(personId);
    }

    @Override
    public Notification markAsRead(Integer notificationId, Integer personId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("La notificación no existe"));

        // Solo el destinatario puede marcarla como leída: no se confirma que exista si no es suya.
        if (!notification.getPersonId().equals(personId)) {
            throw new ResourceNotFoundException("La notificación no existe");
        }

        return notificationRepository.save(notification.markAsRead());
    }
}
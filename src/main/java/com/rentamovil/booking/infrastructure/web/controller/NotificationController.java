package com.rentamovil.booking.infrastructure.web.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rentamovil.booking.domain.entity.AuthenticatedUser;
import com.rentamovil.booking.infrastructure.web.auth.CurrentUser;
import com.rentamovil.booking.domain.port.in.NotificationUseCases;
import com.rentamovil.booking.infrastructure.web.dto.NotificationResponse;

import lombok.RequiredArgsConstructor;

/** HU-NOTIF-001: la campanita de notificaciones in-app. */
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationUseCases notifications;

    @GetMapping
    public List<NotificationResponse> findMine(@CurrentUser AuthenticatedUser currentUser) {
        return notifications.findMine(currentUser.getPersonId())
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }

    /** Marca como leída. Solo su propio destinatario puede. */
    @PatchMapping("/{id}/read")
    public NotificationResponse markAsRead(@CurrentUser AuthenticatedUser currentUser,
                                           @PathVariable Integer id) {
        return NotificationResponse.from(notifications.markAsRead(id, currentUser.getPersonId()));
    }
}
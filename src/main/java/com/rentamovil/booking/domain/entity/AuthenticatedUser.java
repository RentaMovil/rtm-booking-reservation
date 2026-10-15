package com.rentamovil.booking.domain.entity;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Quién hace la petición, según los claims del access token.
 *
 * <p>personId es el titular de la reserva: reservation.client_id apunta a iam.person, no a
 * iam.[user] (INV-001 y data-dictionary.md "deliberate absence"). Por eso se necesitan ambos ids.
 */
@Getter
@AllArgsConstructor
public class AuthenticatedUser {

    /** Claim sub. */
    private Integer userId;

    /** Claim personId. Ver README → "Pendiente: el claim personId en el token". */
    private Integer personId;

    private String role;

    private List<String> permissions;

    public boolean hasPermission(String permission) {
        return permissions != null && permissions.contains(permission);
    }

    public boolean isAdmin() {
        return "ADMIN".equals(role) || "SUPER_ADMIN".equals(role);
    }
}
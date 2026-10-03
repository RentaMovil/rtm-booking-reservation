package com.rentamovil.booking.domain.port.out;

/**
 * Registra una acción en shared.audit (cross-cutting, la crea rtm-iam-db). Cada servicio añade su
 * propio &lt;schema&gt;_app con permiso de INSERT; ver rtm-booking-reservation-db/03_dcl.
 */
public interface AuditPort {

    void record(Integer userId, String action, String affectedTable, Integer affectedRecordId,
                String detailJson);

    /** Acción sin actor humano, como la expiración automática de INV-015. */
    void recordSystem(String action, String affectedTable, Integer affectedRecordId, String detailJson);
}
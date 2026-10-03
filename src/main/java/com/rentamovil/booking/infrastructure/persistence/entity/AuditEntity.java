package com.rentamovil.booking.infrastructure.persistence.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Mapeo de shared.audit (solo INSERT: append-only, la aplicación nunca la lee).
 * La crea rtm-iam-db; este servicio solo tiene permiso de INSERT.
 */
@Entity
@Table(name = "audit")
@Getter
@Setter
@NoArgsConstructor
public class AuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id")
    private Integer auditId;

    /** NULL = acción de sistema o anónima, como la expiración automática de INV-015. */
    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Column(name = "affected_table", nullable = false, length = 50)
    private String affectedTable;

    @Column(name = "affected_record_id")
    private Integer affectedRecordId;

    @Column(name = "date", nullable = false)
    private LocalDateTime date;

    /** JSON validado por ck_audit_detail ISJSON(detail) = 1. */
    @Column(name = "detail")
    private String detail;
}
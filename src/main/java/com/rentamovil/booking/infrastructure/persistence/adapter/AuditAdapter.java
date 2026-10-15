package com.rentamovil.booking.infrastructure.persistence.adapter;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.rentamovil.booking.domain.port.out.AuditPort;
import com.rentamovil.booking.infrastructure.persistence.entity.AuditEntity;
import com.rentamovil.booking.infrastructure.persistence.repository.AuditJpaRepository;

/**
 * Escribe en shared.audit, que crea rtm-iam-db.
 *
 * <p>Si el insert falla, la operación de negocio que ya tuvo éxito no se revierte: se registra
 * el fallo en el log y se sigue. La auditoría no debe tumbar el sistema.
 */
@Component
public class AuditAdapter implements AuditPort {

    private final AuditJpaRepository auditRepository;

    public AuditAdapter(AuditJpaRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    @Override
    public void record(Integer userId, String action, String affectedTable, Integer affectedRecordId,
                       String detailJson) {
        persist(userId, action, affectedTable, affectedRecordId, detailJson);
    }

    @Override
    public void recordSystem(String action, String affectedTable, Integer affectedRecordId,
                             String detailJson) {
        persist(null, action, affectedTable, affectedRecordId, detailJson);
    }

    // Sin @Transactional a propósito: los métodos se llaman entre sí dentro de esta clase y Spring
    // no intercepta llamadas internas (self-invocation), así que la anotación no se aplicaría.
    // SimpleJpaRepository.save ya abre su propia transacción por operación de repositorio.
    private void persist(Integer userId, String action, String affectedTable, Integer affectedRecordId,
                         String detailJson) {
        try {
            AuditEntity entity = new AuditEntity();
            entity.setUserId(userId);
            entity.setAction(action);
            entity.setAffectedTable(affectedTable);
            entity.setAffectedRecordId(affectedRecordId);
            entity.setDate(LocalDateTime.now());
            entity.setDetail(detailJson);
            auditRepository.save(entity);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(AuditAdapter.class)
                    .error("No se pudo escribir la auditoría {} de {}#{}", action, affectedTable,
                            affectedRecordId, e);
        }
    }
}
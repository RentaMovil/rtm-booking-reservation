package com.rentamovil.booking.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.rentamovil.booking.domain.entity.Notification;
import com.rentamovil.booking.domain.port.out.NotificationRepositoryPort;
import com.rentamovil.booking.infrastructure.persistence.repository.NotificationJpaRepository;

@Component
public class NotificationRepositoryAdapter implements NotificationRepositoryPort {

    private final NotificationJpaRepository jpaRepository;

    public NotificationRepositoryAdapter(NotificationJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public Notification save(Notification notification) {
        return PersistenceMapper.toDomain(jpaRepository.save(PersistenceMapper.toEntity(notification)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Notification> findById(Integer id) {
        return jpaRepository.findById(id).map(PersistenceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> findByPersonId(Integer personId) {
        return jpaRepository.findByPersonIdOrderBySentDateDesc(personId)
                .stream()
                .map(PersistenceMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnread(Integer personId) {
        return jpaRepository.countByPersonIdAndReadFalse(personId);
    }
}
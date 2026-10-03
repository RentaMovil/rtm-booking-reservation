package com.rentamovil.booking.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.rentamovil.booking.domain.entity.Rental;
import com.rentamovil.booking.domain.enums.RentalStatus;
import com.rentamovil.booking.domain.port.out.RentalRepositoryPort;
import com.rentamovil.booking.infrastructure.persistence.repository.RentalJpaRepository;

@Component
public class RentalRepositoryAdapter implements RentalRepositoryPort {

    private final RentalJpaRepository jpaRepository;

    public RentalRepositoryAdapter(RentalJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public Rental save(Rental rental) {
        return PersistenceMapper.toDomain(jpaRepository.save(PersistenceMapper.toEntity(rental)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Rental> findById(Integer id) {
        return jpaRepository.findById(id).map(PersistenceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Rental> findByReservationId(Integer reservationId) {
        return jpaRepository.findByReservationId(reservationId).map(PersistenceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Rental> findInProgress() {
        return jpaRepository.findByStatusOrderByActualStartDateAsc(RentalStatus.IN_PROGRESS.name())
                .stream()
                .map(PersistenceMapper::toDomain)
                .toList();
    }
}
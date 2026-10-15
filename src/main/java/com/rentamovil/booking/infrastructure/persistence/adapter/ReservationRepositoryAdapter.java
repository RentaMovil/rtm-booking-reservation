package com.rentamovil.booking.infrastructure.persistence.adapter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.rentamovil.booking.domain.entity.Reservation;
import com.rentamovil.booking.domain.enums.ReservationStatus;
import com.rentamovil.booking.domain.port.out.ReservationRepositoryPort;
import com.rentamovil.booking.infrastructure.persistence.entity.ReservationEntity;
import com.rentamovil.booking.infrastructure.persistence.repository.ReservationJpaRepository;

@Component
public class ReservationRepositoryAdapter implements ReservationRepositoryPort {

    private final ReservationJpaRepository jpaRepository;

    public ReservationRepositoryAdapter(ReservationJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public Reservation save(Reservation reservation) {
        return PersistenceMapper.toDomain(jpaRepository.save(PersistenceMapper.toEntity(reservation)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Reservation> findById(Integer id) {
        return jpaRepository.findById(id).map(PersistenceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Reservation> findByClientId(Integer clientId) {
        return jpaRepository.findByClientIdOrderByReservationDateDesc(clientId)
                .stream()
                .map(PersistenceMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Reservation> findAll(ReservationStatus statusFilter) {
        List<ReservationEntity> entities = statusFilter == null
                ? jpaRepository.findAllByOrderByReservationDateDesc()
                : jpaRepository.findByStatusOrderByReservationDateAsc(statusFilter.name());
        return entities.stream().map(PersistenceMapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OccupiedRange> findOccupiedRangesByVehicle(Integer vehicleId, LocalDateTime from,
                                                           LocalDateTime to) {
        return jpaRepository.findOccupiedRanges(vehicleId, from, to)
                .stream()
                .map(entity -> new OccupiedRange(entity.getId(), entity.getStartDate(), entity.getEndDate()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Reservation> findExpiredUnpaid(LocalDateTime cutoff) {
        return jpaRepository.findExpiredUnpaid(cutoff)
                .stream()
                .map(PersistenceMapper::toDomain)
                .toList();
    }
}
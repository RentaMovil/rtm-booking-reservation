package com.rentamovil.booking.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.rentamovil.booking.domain.entity.InsuranceType;
import com.rentamovil.booking.domain.port.out.InsuranceTypeRepositoryPort;
import com.rentamovil.booking.infrastructure.persistence.repository.InsuranceTypeJpaRepository;

@Component
public class InsuranceTypeRepositoryAdapter implements InsuranceTypeRepositoryPort {

    private final InsuranceTypeJpaRepository jpaRepository;

    public InsuranceTypeRepositoryAdapter(InsuranceTypeJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public InsuranceType save(InsuranceType insuranceType) {
        return PersistenceMapper.toDomain(jpaRepository.save(PersistenceMapper.toEntity(insuranceType)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<InsuranceType> findById(Integer id) {
        return jpaRepository.findById(id).map(PersistenceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InsuranceType> findAll() {
        return jpaRepository.findAllByOrderByDailyCostAsc()
                .stream()
                .map(PersistenceMapper::toDomain)
                .toList();
    }
}
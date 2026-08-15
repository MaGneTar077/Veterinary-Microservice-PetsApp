package com.MyAnimaLog.Veterinary.infrastructure.adapters;

import com.MyAnimaLog.Veterinary.application.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.model.VeterinaryEmployee;
import com.MyAnimaLog.Veterinary.infrastructure.mapper.VeterinaryEmployeeMapper;
import com.MyAnimaLog.Veterinary.infrastructure.repositories.VeterinaryEmployeeJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class VeterinaryEmployeeRepositoryAdapter implements VeterinaryEmployeeRepositoryPort {

    private final VeterinaryEmployeeJpaRepository jpaRepository;
    private final VeterinaryEmployeeMapper mapper;

    @Override
    public VeterinaryEmployee save(VeterinaryEmployee employee) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(employee)));
    }

    @Override
    public boolean existsByVeterinaryIdAndUserId(UUID veterinaryId, UUID userId) {
        return jpaRepository.existsByVeterinaryIdAndUserId(veterinaryId, userId);
    }

    @Override
    public Optional<VeterinaryEmployee> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

}

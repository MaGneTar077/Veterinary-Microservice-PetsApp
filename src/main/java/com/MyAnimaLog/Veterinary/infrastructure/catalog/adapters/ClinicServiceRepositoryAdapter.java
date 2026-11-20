package com.MyAnimaLog.Veterinary.infrastructure.catalog.adapters;

import com.MyAnimaLog.Veterinary.application.catalog.ports.out.ClinicServiceRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.catalog.model.ClinicService;
import com.MyAnimaLog.Veterinary.infrastructure.catalog.mapper.ClinicServiceMapper;
import com.MyAnimaLog.Veterinary.infrastructure.catalog.repositories.ClinicServiceJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ClinicServiceRepositoryAdapter implements ClinicServiceRepositoryPort {

    private final ClinicServiceJpaRepository jpaRepository;
    private final ClinicServiceMapper mapper;

    @Override
    public ClinicService save(ClinicService service) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(service)));
    }

    @Override
    public Optional<ClinicService> findByVeterinaryIdAndId(UUID veterinaryId, UUID serviceId) {
        return jpaRepository.findByVeterinaryIdAndId(veterinaryId, serviceId).map(mapper::toDomain);
    }

    @Override
    public List<ClinicService> findByVeterinaryId(UUID veterinaryId) {
        return jpaRepository.findByVeterinaryId(veterinaryId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<ClinicService> findByVeterinaryIdAndActiveTrue(UUID veterinaryId) {
        return jpaRepository.findByVeterinaryIdAndActiveTrue(veterinaryId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public boolean existsActiveByVeterinaryIdAndName(UUID veterinaryId, String name) {
        return jpaRepository.existsByVeterinaryIdAndActiveTrueAndNameIgnoreCase(veterinaryId, name);
    }
}

package com.MyAnimaLog.Veterinary.infrastructure.professional.adapters;

import com.MyAnimaLog.Veterinary.application.professional.ports.out.ProfessionalProfileRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.professional.model.ProfessionalProfile;
import com.MyAnimaLog.Veterinary.infrastructure.professional.mapper.ProfessionalProfileMapper;
import com.MyAnimaLog.Veterinary.infrastructure.professional.repositories.ProfessionalProfileJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProfessionalProfileRepositoryAdapter implements ProfessionalProfileRepositoryPort {

    private final ProfessionalProfileJpaRepository jpaRepository;
    private final ProfessionalProfileMapper mapper;

    @Override
    public Optional<ProfessionalProfile> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<ProfessionalProfile> findByUserId(UUID userId) {
        return jpaRepository.findByUserId(userId).map(mapper::toDomain);
    }

    @Override
    public boolean existsByLicenseNumber(String licenseNumber) {
        return jpaRepository.existsByLicenseNumber(licenseNumber);
    }

    @Override
    public ProfessionalProfile save(ProfessionalProfile profile) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(profile)));
    }
}

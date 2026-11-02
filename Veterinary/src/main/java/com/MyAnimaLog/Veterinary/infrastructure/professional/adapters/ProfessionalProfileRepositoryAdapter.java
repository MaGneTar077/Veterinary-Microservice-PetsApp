package com.MyAnimaLog.Veterinary.infrastructure.professional.adapters;

import com.MyAnimaLog.Veterinary.application.professional.ports.out.ProfessionalProfileRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.professional.model.ProfessionalProfile;
import com.MyAnimaLog.Veterinary.infrastructure.professional.entity.ProfessionalProfileEntity;
import com.MyAnimaLog.Veterinary.infrastructure.professional.mapper.ProfessionalProfileMapper;
import com.MyAnimaLog.Veterinary.infrastructure.professional.repositories.ProfessionalProfileJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
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
    public boolean existsByLicenseNumberAndUserIdNot(String licenseNumber, UUID userId) {
        return jpaRepository.existsByLicenseNumberAndUserIdNot(licenseNumber, userId);
    }

    @Override
    public ProfessionalProfile save(ProfessionalProfile profile) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(profile)));
    }

    @Override
    public AdminProfessionalPage findForAdminList(Boolean verified, String search, int page, int size) {
        String likeSearch = search != null ? "%" + search.toLowerCase() + "%" : null;
        Page<ProfessionalProfileEntity> result = jpaRepository.findForAdminList(verified, likeSearch, PageRequest.of(page, size));
        List<ProfessionalProfile> items = result.getContent().stream().map(mapper::toDomain).toList();
        return new AdminProfessionalPage(items, result.getTotalElements());
    }
}

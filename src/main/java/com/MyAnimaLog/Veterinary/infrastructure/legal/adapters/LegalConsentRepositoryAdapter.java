package com.MyAnimaLog.Veterinary.infrastructure.legal.adapters;

import com.MyAnimaLog.Veterinary.application.legal.ports.out.LegalConsentRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.legal.model.LegalConsent;
import com.MyAnimaLog.Veterinary.infrastructure.legal.mapper.LegalConsentMapper;
import com.MyAnimaLog.Veterinary.infrastructure.legal.repositories.LegalConsentJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class LegalConsentRepositoryAdapter implements LegalConsentRepositoryPort {

    private final LegalConsentJpaRepository jpaRepository;
    private final LegalConsentMapper mapper;

    @Override
    public List<LegalConsent> findByUserId(UUID userId) {
        return jpaRepository.findByUserId(userId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public LegalConsent save(LegalConsent consent) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(consent)));
    }
}

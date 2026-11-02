package com.MyAnimaLog.Veterinary.infrastructure.legal.mapper;

import com.MyAnimaLog.Veterinary.domain.legal.model.LegalConsent;
import com.MyAnimaLog.Veterinary.infrastructure.legal.entity.LegalConsentEntity;
import org.springframework.stereotype.Component;

@Component
public class LegalConsentMapper {

    public LegalConsentEntity toEntity(LegalConsent domain) {
        return LegalConsentEntity.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .veterinaryId(domain.getVeterinaryId())
                .consentType(domain.getConsentType())
                .version(domain.getVersion())
                .acceptedAt(domain.getAcceptedAt())
                .build();
    }

    public LegalConsent toDomain(LegalConsentEntity entity) {
        return LegalConsent.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .veterinaryId(entity.getVeterinaryId())
                .consentType(entity.getConsentType())
                .version(entity.getVersion())
                .acceptedAt(entity.getAcceptedAt())
                .build();
    }
}

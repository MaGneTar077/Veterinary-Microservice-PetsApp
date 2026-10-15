package com.MyAnimaLog.Veterinary.infrastructure.professional.mapper;

import com.MyAnimaLog.Veterinary.domain.professional.model.ProfessionalProfile;
import com.MyAnimaLog.Veterinary.infrastructure.professional.entity.ProfessionalProfileEntity;
import org.springframework.stereotype.Component;

@Component
public class ProfessionalProfileMapper {

    public ProfessionalProfileEntity toEntity(ProfessionalProfile domain) {
        return ProfessionalProfileEntity.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .fullName(domain.getFullName())
                .documentType(domain.getDocumentType())
                .documentNumber(domain.getDocumentNumber())
                .licenseNumber(domain.getLicenseNumber())
                .profession(domain.getProfession())
                .specialty(domain.getSpecialty())
                .university(domain.getUniversity())
                .signatureUrl(domain.getSignatureUrl())
                .licenseStatus(domain.getLicenseStatus())
                .rejectionReason(domain.getRejectionReason())
                .verifiedAt(domain.getVerifiedAt())
                .verifiedBy(domain.getVerifiedBy())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    public ProfessionalProfile toDomain(ProfessionalProfileEntity entity) {
        return ProfessionalProfile.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .fullName(entity.getFullName())
                .documentType(entity.getDocumentType())
                .documentNumber(entity.getDocumentNumber())
                .licenseNumber(entity.getLicenseNumber())
                .profession(entity.getProfession())
                .specialty(entity.getSpecialty())
                .university(entity.getUniversity())
                .signatureUrl(entity.getSignatureUrl())
                .licenseStatus(entity.getLicenseStatus())
                .rejectionReason(entity.getRejectionReason())
                .verifiedAt(entity.getVerifiedAt())
                .verifiedBy(entity.getVerifiedBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

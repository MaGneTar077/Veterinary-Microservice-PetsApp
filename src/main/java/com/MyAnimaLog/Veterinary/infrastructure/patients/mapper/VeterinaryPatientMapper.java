package com.MyAnimaLog.Veterinary.infrastructure.patients.mapper;

import com.MyAnimaLog.Veterinary.domain.patients.model.VeterinaryPatient;
import com.MyAnimaLog.Veterinary.infrastructure.patients.entity.VeterinaryPatientEntity;
import org.springframework.stereotype.Component;

@Component
public class VeterinaryPatientMapper {

    /**
     * {@code updatedAt} no se copia al escribir: lo genera Hibernate con {@code @UpdateTimestamp}
     * (ver {@link VeterinaryPatientEntity#getUpdatedAt()}), así que mandar un valor acá no tendría
     * efecto y solo invitaría a confundirse.
     */
    public VeterinaryPatientEntity toEntity(VeterinaryPatient domain) {
        return VeterinaryPatientEntity.builder()
                .id(domain.getId())
                .veterinaryId(domain.getVeterinaryId())
                .petId(domain.getPetId())
                .origin(domain.getOrigin())
                .consentStatus(domain.getConsentStatus())
                .grantedBy(domain.getGrantedBy())
                .grantedAt(domain.getGrantedAt())
                .revokedAt(domain.getRevokedAt())
                .petName(domain.getPetName())
                .species(domain.getSpecies())
                .createdAt(domain.getCreatedAt())
                .build();
    }

    public VeterinaryPatient toDomain(VeterinaryPatientEntity entity) {
        return VeterinaryPatient.builder()
                .id(entity.getId())
                .veterinaryId(entity.getVeterinaryId())
                .petId(entity.getPetId())
                .origin(entity.getOrigin())
                .consentStatus(entity.getConsentStatus())
                .grantedBy(entity.getGrantedBy())
                .grantedAt(entity.getGrantedAt())
                .revokedAt(entity.getRevokedAt())
                .petName(entity.getPetName())
                .species(entity.getSpecies())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

package com.MyAnimaLog.Veterinary.infrastructure.patients.adapters;

import com.MyAnimaLog.Veterinary.application.patients.ports.out.VeterinaryPatientRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.patients.enums.ConsentStatus;
import com.MyAnimaLog.Veterinary.domain.patients.model.VeterinaryPatient;
import com.MyAnimaLog.Veterinary.infrastructure.patients.mapper.VeterinaryPatientMapper;
import com.MyAnimaLog.Veterinary.infrastructure.patients.repositories.VeterinaryPatientJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class VeterinaryPatientRepositoryAdapter implements VeterinaryPatientRepositoryPort {

    private final VeterinaryPatientJpaRepository jpaRepository;
    private final VeterinaryPatientMapper mapper;

    @Override
    public VeterinaryPatient save(VeterinaryPatient patient) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(patient)));
    }

    @Override
    public Optional<VeterinaryPatient> findByVeterinaryIdAndPetId(UUID veterinaryId, UUID petId) {
        return jpaRepository.findByVeterinaryIdAndPetId(veterinaryId, petId).map(mapper::toDomain);
    }

    @Override
    public List<VeterinaryPatient> findGrantedByVeterinaryIdAndGrantedBy(UUID veterinaryId, UUID grantedBy) {
        return jpaRepository.findByVeterinaryIdAndGrantedByAndConsentStatus(
                        veterinaryId, grantedBy, ConsentStatus.GRANTED).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public long countGrantedByVeterinaryIdAndGrantedBy(UUID veterinaryId, UUID grantedBy) {
        return jpaRepository.countByVeterinaryIdAndGrantedByAndConsentStatus(
                veterinaryId, grantedBy, ConsentStatus.GRANTED);
    }

    @Override
    public List<VeterinaryPatient> findByVeterinaryIdAndPetIdIn(UUID veterinaryId, List<UUID> petIds) {
        if (petIds == null || petIds.isEmpty()) {
            return List.of();
        }
        return jpaRepository.findByVeterinaryIdAndPetIdIn(veterinaryId, petIds).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<VeterinaryPatient> findGrantedByPetId(UUID petId) {
        return jpaRepository.findByPetIdAndConsentStatus(petId, ConsentStatus.GRANTED).stream()
                .map(mapper::toDomain)
                .toList();
    }
}

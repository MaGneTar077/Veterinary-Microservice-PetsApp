package com.MyAnimaLog.Veterinary.infrastructure.patients.repositories;

import com.MyAnimaLog.Veterinary.domain.patients.enums.ConsentStatus;
import com.MyAnimaLog.Veterinary.infrastructure.patients.entity.VeterinaryPatientEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VeterinaryPatientJpaRepository extends JpaRepository<VeterinaryPatientEntity, UUID> {

    /** Como máximo una fila por el índice único {@code ux_patient_vet_pet}. */
    Optional<VeterinaryPatientEntity> findByVeterinaryIdAndPetId(UUID veterinaryId, UUID petId);

    List<VeterinaryPatientEntity> findByVeterinaryIdAndGrantedByAndConsentStatus(
            UUID veterinaryId, UUID grantedBy, ConsentStatus consentStatus);

    long countByVeterinaryIdAndGrantedByAndConsentStatus(
            UUID veterinaryId, UUID grantedBy, ConsentStatus consentStatus);

    List<VeterinaryPatientEntity> findByVeterinaryIdAndPetIdIn(UUID veterinaryId, List<UUID> petIds);

    List<VeterinaryPatientEntity> findByPetIdAndConsentStatus(UUID petId, ConsentStatus consentStatus);
}

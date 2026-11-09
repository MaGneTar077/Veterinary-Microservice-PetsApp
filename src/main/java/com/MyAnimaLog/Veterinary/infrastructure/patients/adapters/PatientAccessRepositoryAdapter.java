package com.MyAnimaLog.Veterinary.infrastructure.patients.adapters;

import com.MyAnimaLog.Veterinary.application.patients.ports.out.PatientAccessRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.infrastructure.patients.repositories.PatientAccessJpaRepository;
import com.MyAnimaLog.Veterinary.infrastructure.patients.repositories.PatientAccessProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PatientAccessRepositoryAdapter implements PatientAccessRepositoryPort {

    private final PatientAccessJpaRepository jpaRepository;

    @Override
    public Optional<AccessRow> findAccess(UUID veterinaryId, UUID petId, UUID userId) {
        return jpaRepository.findAccess(veterinaryId, petId, userId).map(this::toRow);
    }

    private AccessRow toRow(PatientAccessProjection projection) {
        return new AccessRow(
                VeterinaryStatus.valueOf(projection.getVeterinaryStatus()),
                projection.getEmployeeId(),
                projection.getRole(),
                projection.getEmployeeActive(),
                projection.getLicenseStatus(),
                projection.getPetConsentStatus());
    }
}

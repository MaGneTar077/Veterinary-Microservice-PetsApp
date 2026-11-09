package com.MyAnimaLog.Veterinary.application.patients.services;

import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.patients.dto.RevokePetShareResponse;
import com.MyAnimaLog.Veterinary.application.patients.ports.in.RevokePetShareUseCase;
import com.MyAnimaLog.Veterinary.application.patients.ports.out.VeterinaryPatientRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.dto.DomainEvent;
import com.MyAnimaLog.Veterinary.application.shared.dto.EventMetadata;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.AuthenticatedUserPort;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.EventPublisherPort;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.patients.enums.ConsentStatus;
import com.MyAnimaLog.Veterinary.domain.patients.exceptions.PetNotOwnedException;
import com.MyAnimaLog.Veterinary.domain.patients.model.VeterinaryPatient;
import com.MyAnimaLog.Veterinary.domain.shared.enums.VeterinaryEventType;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VeterinaryNotFoundException;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * El dueño le quita a una clínica el acceso a una mascota.
 *
 * <p><strong>No llama a Pets.</strong> La autorización sale de {@code granted_by}, que es nuestro
 * propio registro de quién dio el consentimiento. Es deliberado: revocar un acceso a datos clínicos
 * tiene que funcionar incluso con Pets caído. Si dependiera de un {@code isOwner} remoto, una caída
 * de Pets dejaría al dueño sin poder cortar el acceso — el peor momento para no poder.
 *
 * <p>Revocar es idempotente: si el consentimiento ya estaba revocado responde {@code 200} con
 * {@code alreadyRevoked} y no publica evento. El resultado que el dueño pidió ya se cumple.
 */
@Service
@RequiredArgsConstructor
public class RevokePetShareService implements RevokePetShareUseCase {

    private final VeterinaryRepositoryPort veterinaryRepositoryPort;
    private final VeterinaryPatientRepositoryPort patientRepositoryPort;
    private final VeterinaryEmployeeRepositoryPort employeeRepositoryPort;
    private final AuthenticatedUserPort authenticatedUserPort;
    private final EventPublisherPort eventPublisherPort;

    @Override
    @Transactional
    public RevokePetShareResponse revoke(UUID veterinaryId, UUID petId) {
        UUID userId = authenticatedUserPort.current().userId();

        Veterinary veterinary = veterinaryRepositoryPort.findById(veterinaryId)
                .orElseThrow(VeterinaryNotFoundException::new);

        // Mismo 403 si la fila no existe que si la compartió otra persona: distinguirlos diría si
        // esa mascota está compartida con esa clínica, que no es información del caller.
        // No exige la clínica ACTIVE: cortar el acceso tiene que poder hacerse siempre.
        VeterinaryPatient patient = patientRepositoryPort
                .findByVeterinaryIdAndPetId(veterinaryId, petId)
                .filter(row -> userId.equals(row.getGrantedBy()))
                .orElseThrow(PetNotOwnedException::new);

        if (!patient.isGranted()) {
            return RevokePetShareResponse.builder()
                    .veterinaryId(veterinaryId)
                    .petId(petId)
                    .petName(patient.getPetName())
                    .revokedAt(patient.getRevokedAt())
                    .alreadyRevoked(true)
                    .build();
        }

        LocalDateTime now = LocalDateTime.now();
        VeterinaryPatient revoked = patientRepositoryPort.save(patient.toBuilder()
                .consentStatus(ConsentStatus.REVOKED)
                .revokedAt(now)
                .build());

        publishRevokedEvent(veterinary, revoked, userId);

        return RevokePetShareResponse.builder()
                .veterinaryId(veterinaryId)
                .petId(petId)
                .petName(revoked.getPetName())
                .revokedAt(revoked.getRevokedAt())
                .alreadyRevoked(false)
                .build();
    }

    /** Al OWNER activo de la clínica: es quien necesita saber que perdió el acceso a ese paciente. */
    private void publishRevokedEvent(Veterinary veterinary, VeterinaryPatient patient, UUID userId) {
        employeeRepositoryPort.findByVeterinaryIdAndRoleAndActiveTrue(veterinary.getId(), EmployeeRole.OWNER)
                .ifPresent(owner -> {
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("veterinaryName", veterinary.getName());
                    payload.put("petId", patient.getPetId());
                    payload.put("petName", patient.getPetName());
                    payload.put("clientUserId", userId);
                    eventPublisherPort.publish(DomainEvent.create(
                            VeterinaryEventType.PET_SHARE_REVOKED,
                            owner.getUserId(),
                            null,
                            payload,
                            EventMetadata.of(veterinary.getId(), null)));
                });
    }
}

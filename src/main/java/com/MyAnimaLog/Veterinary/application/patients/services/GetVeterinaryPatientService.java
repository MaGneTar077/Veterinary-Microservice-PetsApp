package com.MyAnimaLog.Veterinary.application.patients.services;

import com.MyAnimaLog.Veterinary.application.patients.dto.GetVeterinaryPatientResponse;
import com.MyAnimaLog.Veterinary.application.patients.ports.in.GetVeterinaryPatientUseCase;
import com.MyAnimaLog.Veterinary.application.patients.ports.out.VeterinaryPatientRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.PetsServicePort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.domain.patients.exceptions.PatientNotFoundException;
import com.MyAnimaLog.Veterinary.domain.patients.model.VeterinaryPatient;
import com.MyAnimaLog.Veterinary.domain.security.Permission;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Detalle de un paciente — solo si está compartido <strong>activamente</strong> con la clínica; si
 * no (nunca se compartió, o se revocó), 404. Igual que el listado, los datos de la mascota salen de
 * Pets en vivo.
 */
@Service
@RequiredArgsConstructor
public class GetVeterinaryPatientService implements GetVeterinaryPatientUseCase {

    private final VeterinaryAuthorizationService authorizationService;
    private final VeterinaryPatientRepositoryPort patientRepositoryPort;
    private final PetsServicePort petsServicePort;

    @Override
    public GetVeterinaryPatientResponse get(UUID veterinaryId, UUID petId) {
        authorizationService.require(veterinaryId, Permission.CLINICAL_READ_BASIC);

        VeterinaryPatient patient = patientRepositoryPort.findByVeterinaryIdAndPetId(veterinaryId, petId)
                .filter(VeterinaryPatient::isGranted)
                .orElseThrow(PatientNotFoundException::new);

        PetsServicePort.PetInfo pet = petsServicePort.getPets(List.of(petId)).get(petId);
        if (pet == null) {
            throw new PatientNotFoundException();
        }

        return GetVeterinaryPatientResponse.builder()
                .petId(pet.id())
                .name(pet.name())
                .species(pet.species())
                .breed(pet.breed())
                .sex(pet.sex())
                .birthDate(pet.birthDate())
                .photoUrl(pet.photoUrl())
                .ownerUserId(pet.ownerId())
                .grantedAt(patient.getGrantedAt())
                .build();
    }
}

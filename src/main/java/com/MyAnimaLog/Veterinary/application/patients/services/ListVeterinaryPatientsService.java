package com.MyAnimaLog.Veterinary.application.patients.services;

import com.MyAnimaLog.Veterinary.application.patients.dto.ListVeterinaryPatientsResponse;
import com.MyAnimaLog.Veterinary.application.patients.ports.in.ListVeterinaryPatientsUseCase;
import com.MyAnimaLog.Veterinary.application.patients.ports.out.VeterinaryPatientRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.PetsServicePort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.domain.patients.model.VeterinaryPatient;
import com.MyAnimaLog.Veterinary.domain.security.Permission;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * Pacientes (mascotas compartidas activamente) de la clínica, paginado. Los datos de cada mascota
 * —nombre, especie, raza, foto— salen de Pets <strong>en vivo</strong>, en <strong>una sola
 * llamada en lote</strong> para toda la página (nunca una por fila). Si Pets no responde, la
 * excepción de {@code PetsServicePort} se propaga tal cual (503): mostrar el snapshot viejo en su
 * lugar daría información potencialmente desactualizada como si fuera actual.
 */
@Service
@RequiredArgsConstructor
public class ListVeterinaryPatientsService implements ListVeterinaryPatientsUseCase {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;

    private final VeterinaryAuthorizationService authorizationService;
    private final VeterinaryPatientRepositoryPort patientRepositoryPort;
    private final PetsServicePort petsServicePort;

    @Override
    public ListVeterinaryPatientsResponse list(UUID veterinaryId, Integer page, Integer size) {
        authorizationService.require(veterinaryId, Permission.CLINICAL_READ_BASIC);

        int effectivePage = page != null ? page : 0;
        int effectiveSize = size != null ? Math.min(size, MAX_SIZE) : DEFAULT_SIZE;

        VeterinaryPatientRepositoryPort.PatientsPage result =
                patientRepositoryPort.findGrantedByVeterinaryId(veterinaryId, effectivePage, effectiveSize);

        Map<UUID, PetsServicePort.PetInfo> pets = petsServicePort.getPets(
                result.items().stream().map(VeterinaryPatient::getPetId).distinct().toList());

        return ListVeterinaryPatientsResponse.builder()
                .items(result.items().stream()
                        .map(patient -> toDto(patient, pets.get(patient.getPetId())))
                        .filter(java.util.Objects::nonNull)
                        .toList())
                .page(effectivePage)
                .size(effectiveSize)
                .total(result.total())
                .build();
    }

    /**
     * {@code null} si Pets no conoce esa mascota (borrada del lado de Pets sin que se haya
     * revocado el consentimiento todavía): se omite de la página en vez de romperla con un dato a
     * medias.
     */
    private ListVeterinaryPatientsResponse.Patient toDto(VeterinaryPatient patient,
                                                           PetsServicePort.PetInfo pet) {
        if (pet == null) {
            return null;
        }
        return ListVeterinaryPatientsResponse.Patient.builder()
                .petId(pet.id())
                .name(pet.name())
                .species(pet.species())
                .breed(pet.breed())
                .photoUrl(pet.photoUrl())
                .ownerUserId(pet.ownerId())
                .grantedAt(patient.getGrantedAt())
                .build();
    }
}

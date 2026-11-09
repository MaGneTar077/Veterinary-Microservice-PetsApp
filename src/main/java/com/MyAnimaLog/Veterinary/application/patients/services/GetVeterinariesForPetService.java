package com.MyAnimaLog.Veterinary.application.patients.services;

import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.patients.dto.GetVeterinariesForPetResponse;
import com.MyAnimaLog.Veterinary.application.patients.ports.in.GetVeterinariesForPetUseCase;
import com.MyAnimaLog.Veterinary.application.patients.ports.out.VeterinaryPatientRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.AuthenticatedUserPort;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.PetsServicePort;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.patients.exceptions.PetNotOwnedException;
import com.MyAnimaLog.Veterinary.domain.patients.model.VeterinaryPatient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Con qué clínicas comparte el dueño una mascota: la pantalla desde la que puede revocar.
 *
 * <p>Verifica contra Pets que sea el dueño antes de responder. Acá sí se usa
 * {@link PetsServicePort#isOwner} y no el batch, porque es una sola mascota. Y sí se verifica, a
 * diferencia de revocar: esta respuesta dice <strong>qué clínicas</strong> ven una mascota, y eso no
 * se le puede mostrar a alguien que no es su dueño (un EDITOR o VIEWER de Pets tampoco).
 */
@Service
@RequiredArgsConstructor
public class GetVeterinariesForPetService implements GetVeterinariesForPetUseCase {

    private final VeterinaryPatientRepositoryPort patientRepositoryPort;
    private final VeterinaryRepositoryPort veterinaryRepositoryPort;
    private final PetsServicePort petsServicePort;
    private final AuthenticatedUserPort authenticatedUserPort;

    @Override
    public GetVeterinariesForPetResponse list(UUID petId) {
        UUID userId = authenticatedUserPort.current().userId();
        if (!petsServicePort.isOwner(petId, userId)) {
            throw new PetNotOwnedException();
        }

        List<VeterinaryPatient> shares = patientRepositoryPort.findGrantedByPetId(petId);
        Map<UUID, Veterinary> clinics = veterinaryRepositoryPort
                .findByIdIn(shares.stream().map(VeterinaryPatient::getVeterinaryId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(Veterinary::getId, Function.identity()));

        return GetVeterinariesForPetResponse.builder()
                .petId(petId)
                .veterinaries(shares.stream()
                        .map(share -> toDto(share, clinics.get(share.getVeterinaryId())))
                        .filter(Objects::nonNull)
                        .toList())
                .build();
    }

    private GetVeterinariesForPetResponse.SharedWith toDto(VeterinaryPatient share, Veterinary clinic) {
        if (clinic == null) {
            return null;
        }
        return GetVeterinariesForPetResponse.SharedWith.builder()
                .veterinaryId(clinic.getId())
                .name(clinic.getName())
                .logoUrl(clinic.getLogoUrl())
                .city(clinic.getCity())
                .grantedAt(share.getGrantedAt())
                .build();
    }
}

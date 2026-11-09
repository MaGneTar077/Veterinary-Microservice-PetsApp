package com.MyAnimaLog.Veterinary.application.patients.services;

import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.patients.dto.SharePetsWithVeterinaryRequest;
import com.MyAnimaLog.Veterinary.application.patients.dto.SharePetsWithVeterinaryResponse;
import com.MyAnimaLog.Veterinary.application.patients.ports.in.SharePetsWithVeterinaryUseCase;
import com.MyAnimaLog.Veterinary.application.patients.ports.out.UserVeterinaryLinkRepositoryPort;
import com.MyAnimaLog.Veterinary.application.patients.ports.out.VeterinaryPatientRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.dto.DomainEvent;
import com.MyAnimaLog.Veterinary.application.shared.dto.EventMetadata;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.AuthenticatedUserPort;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.EventPublisherPort;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.PetsServicePort;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.ClinicNotActiveException;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.patients.enums.ConsentStatus;
import com.MyAnimaLog.Veterinary.domain.patients.exceptions.InvalidPetShareException;
import com.MyAnimaLog.Veterinary.domain.patients.exceptions.NotLinkedToVeterinaryException;
import com.MyAnimaLog.Veterinary.domain.patients.exceptions.PetNotOwnedException;
import com.MyAnimaLog.Veterinary.domain.patients.model.VeterinaryPatient;
import com.MyAnimaLog.Veterinary.domain.shared.enums.VeterinaryEventType;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VeterinaryNotFoundException;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.domain.staff.model.VeterinaryEmployee;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * El dueño le da acceso a una clínica a una o varias de sus mascotas. Token normal: el consentimiento
 * es del dueño y no se puede delegar en la clínica.
 *
 * <p><strong>Solo el dueño</strong>: la propiedad se verifica contra Pets en cada llamada, y un
 * EDITOR o VIEWER de la mascota (los roles con los que Pets permite compartir entre personas) nunca
 * pasa el chequeo. Ceder la historia clínica a un tercero es una decisión que solo le corresponde a
 * quien es dueño.
 *
 * <p><strong>Todo o nada</strong>: si alguna de las mascotas pedidas no es del caller —o no existe—
 * no se comparte ninguna. Compartir "las que se pueda" dejaría al dueño creyendo que compartió algo
 * distinto de lo que pidió, que es justo el error que no se puede cometer con datos clínicos.
 *
 * <p>Toda la validación sale de <strong>una sola llamada</strong> a
 * {@link PetsServicePort#getPets(List)}: el batch ya trae el {@code ownerId} de cada mascota, así
 * que no hace falta un {@code isOwner} por mascota, y de paso trae el nombre y la especie para el
 * snapshot.
 */
@Service
@RequiredArgsConstructor
public class SharePetsWithVeterinaryService implements SharePetsWithVeterinaryUseCase {

    private final VeterinaryRepositoryPort veterinaryRepositoryPort;
    private final UserVeterinaryLinkRepositoryPort linkRepositoryPort;
    private final VeterinaryPatientRepositoryPort patientRepositoryPort;
    private final VeterinaryEmployeeRepositoryPort employeeRepositoryPort;
    private final PetsServicePort petsServicePort;
    private final AuthenticatedUserPort authenticatedUserPort;
    private final EventPublisherPort eventPublisherPort;

    @Override
    @Transactional
    public SharePetsWithVeterinaryResponse share(UUID veterinaryId,
                                                  SharePetsWithVeterinaryRequest request) {
        UUID userId = authenticatedUserPort.current().userId();
        List<UUID> petIds = normalizePetIds(request);

        Veterinary veterinary = veterinaryRepositoryPort.findById(veterinaryId)
                .orElseThrow(VeterinaryNotFoundException::new);
        if (veterinary.getStatus() != VeterinaryStatus.ACTIVE) {
            throw new ClinicNotActiveException();
        }
        if (linkRepositoryPort.findActiveLink(userId, veterinaryId).isEmpty()) {
            throw new NotLinkedToVeterinaryException();
        }

        Map<UUID, PetsServicePort.PetInfo> pets = requireAllOwnedBy(petIds, userId);

        LocalDateTime now = LocalDateTime.now();
        Map<UUID, VeterinaryPatient> existing = patientRepositoryPort
                .findByVeterinaryIdAndPetIdIn(veterinaryId, petIds).stream()
                .collect(Collectors.toMap(VeterinaryPatient::getPetId, Function.identity()));

        List<SharePetsWithVeterinaryResponse.SharedPet> shared = new ArrayList<>();
        for (UUID petId : petIds) {
            PetsServicePort.PetInfo pet = pets.get(petId);
            VeterinaryPatient current = existing.get(petId);
            boolean alreadyShared = current != null && current.isGranted();

            VeterinaryPatient saved = patientRepositoryPort.save(
                    grant(current, veterinaryId, userId, pet, now, alreadyShared));

            shared.add(SharePetsWithVeterinaryResponse.SharedPet.builder()
                    .petId(petId)
                    .petName(saved.getPetName())
                    .species(saved.getSpecies())
                    .grantedAt(saved.getGrantedAt())
                    .alreadyShared(alreadyShared)
                    .build());

            if (!alreadyShared) {
                publishSharedEvent(veterinary, saved);
            }
        }

        return SharePetsWithVeterinaryResponse.builder()
                .veterinaryId(veterinaryId)
                .veterinaryName(veterinary.getName())
                .sharedCount((int) shared.stream().filter(pet -> !pet.isAlreadyShared()).count())
                .pets(shared)
                .build();
    }

    private List<UUID> normalizePetIds(SharePetsWithVeterinaryRequest request) {
        List<UUID> petIds = request == null ? null : request.getPetIds();
        if (petIds == null || petIds.isEmpty()) {
            throw new InvalidPetShareException("petIds is required and cannot be empty");
        }
        if (petIds.stream().anyMatch(Objects::isNull)) {
            throw new InvalidPetShareException("petIds cannot contain null values");
        }
        // Repetir un id en el cuerpo no debería producir dos escrituras de la misma fila.
        return petIds.stream().distinct().toList();
    }

    /**
     * Una mascota que no vuelve del batch (no existe, o Pets no la conoce) se trata igual que una de
     * otra persona: mismo {@link PetNotOwnedException} (403). Un 404 para "no existe" permitiría
     * enumerar mascotas probando UUIDs.
     */
    private Map<UUID, PetsServicePort.PetInfo> requireAllOwnedBy(List<UUID> petIds, UUID userId) {
        Map<UUID, PetsServicePort.PetInfo> pets = petsServicePort.getPets(petIds);
        boolean allOwned = petIds.stream()
                .map(pets::get)
                .allMatch(pet -> pet != null && userId.equals(pet.ownerId()));
        if (!allOwned) {
            throw new PetNotOwnedException();
        }
        return pets;
    }

    /**
     * Reactiva la fila existente (mismo {@code id}, conservando {@code origin} y {@code createdAt})
     * o crea una nueva. El índice único {@code ux_patient_vet_pet} no admite dos filas del par, y
     * conservar {@code origin} importa para la Fase 4B: una mascota que registró la clínica y que
     * después el dueño reclamó y comparte sigue siendo {@code REGISTERED_BY_CLINIC}.
     *
     * <p>Si ya estaba {@code GRANTED} se reescriben solo el nombre y la especie (el snapshot puede
     * haber quedado viejo) y se deja el {@code grantedAt} original: el consentimiento no se renovó,
     * ya existía.
     */
    private VeterinaryPatient grant(VeterinaryPatient current, UUID veterinaryId, UUID userId,
                                     PetsServicePort.PetInfo pet, LocalDateTime now,
                                     boolean alreadyShared) {
        if (current == null) {
            return VeterinaryPatient.builder()
                    .id(UUID.randomUUID())
                    .veterinaryId(veterinaryId)
                    .petId(pet.id())
                    .grantedBy(userId)
                    .grantedAt(now)
                    .createdAt(now)
                    .petName(pet.name())
                    .species(pet.species())
                    .build();
        }
        return current.toBuilder()
                .consentStatus(ConsentStatus.GRANTED)
                .grantedBy(userId)
                .grantedAt(alreadyShared ? current.getGrantedAt() : now)
                .revokedAt(null)
                .petName(pet.name())
                .species(pet.species())
                .build();
    }

    /**
     * Un evento <strong>por mascota</strong>, con el payload singular que documenta
     * VETERINARY_SERVICE_PLAN.md §11 ({@code petName}, {@code veterinaryName}). Compartir 3 mascotas
     * publica 3 eventos; agruparlos en uno obligaría a Notifications a manejar una forma distinta
     * de payload para el mismo tipo de evento. {@code sharedCount} va en cada uno para que un
     * consumidor que quiera agrupar pueda hacerlo.
     */
    private void publishSharedEvent(Veterinary veterinary, VeterinaryPatient patient) {
        activeOwner(veterinary.getId()).ifPresent(owner -> {
            Map<String, Object> payload = new HashMap<>();
            payload.put("veterinaryName", veterinary.getName());
            payload.put("petId", patient.getPetId());
            payload.put("petName", patient.getPetName());
            payload.put("species", patient.getSpecies());
            payload.put("clientUserId", patient.getGrantedBy());
            eventPublisherPort.publish(DomainEvent.create(
                    VeterinaryEventType.PET_SHARED_WITH_VETERINARY,
                    owner.getUserId(),
                    null,
                    payload,
                    EventMetadata.of(veterinary.getId(), null)));
        });
    }

    private Optional<VeterinaryEmployee> activeOwner(UUID veterinaryId) {
        return employeeRepositoryPort.findByVeterinaryIdAndRoleAndActiveTrue(
                veterinaryId, EmployeeRole.OWNER);
    }
}

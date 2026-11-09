package com.MyAnimaLog.Veterinary.application.patients.services;

import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.patients.dto.SharePetsWithVeterinaryRequest;
import com.MyAnimaLog.Veterinary.application.patients.dto.SharePetsWithVeterinaryResponse;
import com.MyAnimaLog.Veterinary.application.patients.ports.out.UserVeterinaryLinkRepositoryPort;
import com.MyAnimaLog.Veterinary.application.patients.ports.out.VeterinaryPatientRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.dto.AuthenticatedUser;
import com.MyAnimaLog.Veterinary.application.shared.dto.DomainEvent;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.AuthenticatedUserPort;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.EventPublisherPort;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.PetsServicePort;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.ClinicNotActiveException;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.patients.enums.ConsentStatus;
import com.MyAnimaLog.Veterinary.domain.patients.enums.LinkStatus;
import com.MyAnimaLog.Veterinary.domain.patients.enums.PatientOrigin;
import com.MyAnimaLog.Veterinary.domain.patients.exceptions.InvalidPetShareException;
import com.MyAnimaLog.Veterinary.domain.patients.exceptions.NotLinkedToVeterinaryException;
import com.MyAnimaLog.Veterinary.domain.patients.exceptions.PetNotOwnedException;
import com.MyAnimaLog.Veterinary.domain.patients.model.UserVeterinaryLink;
import com.MyAnimaLog.Veterinary.domain.patients.model.VeterinaryPatient;
import com.MyAnimaLog.Veterinary.domain.shared.enums.VeterinaryEventType;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.ExternalServiceUnavailableException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VeterinaryNotFoundException;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.domain.staff.model.VeterinaryEmployee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SharePetsWithVeterinaryServiceTest {

    @Mock
    private VeterinaryRepositoryPort veterinaryRepositoryPort;

    @Mock
    private UserVeterinaryLinkRepositoryPort linkRepositoryPort;

    @Mock
    private VeterinaryPatientRepositoryPort patientRepositoryPort;

    @Mock
    private VeterinaryEmployeeRepositoryPort employeeRepositoryPort;

    @Mock
    private PetsServicePort petsServicePort;

    @Mock
    private AuthenticatedUserPort authenticatedUserPort;

    @Mock
    private EventPublisherPort eventPublisherPort;

    @InjectMocks
    private SharePetsWithVeterinaryService sharePetsWithVeterinaryService;

    private UUID userId;
    private UUID veterinaryId;
    private UUID ownerUserId;
    private UUID maxId;
    private UUID lunaId;
    private Veterinary clinic;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        veterinaryId = UUID.randomUUID();
        ownerUserId = UUID.randomUUID();
        maxId = UUID.randomUUID();
        lunaId = UUID.randomUUID();

        lenient().when(authenticatedUserPort.current()).thenReturn(
                new AuthenticatedUser(userId, "ana@correo.com", true, false, Optional.empty()));

        clinic = Veterinary.builder()
                .id(veterinaryId)
                .name("Clínica El Bosque")
                .city("Cartagena")
                .tenantId(UUID.randomUUID().toString())
                .status(VeterinaryStatus.ACTIVE)
                .active(true)
                .build();
    }

    private void clinicExists() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(clinic));
    }

    private void userIsLinked() {
        when(linkRepositoryPort.findActiveLink(userId, veterinaryId)).thenReturn(Optional.of(
                UserVeterinaryLink.builder()
                        .id(UUID.randomUUID())
                        .userId(userId)
                        .veterinaryId(veterinaryId)
                        .status(LinkStatus.LINKED)
                        .linkedAt(LocalDateTime.now())
                        .build()));
    }

    private PetsServicePort.PetInfo pet(UUID petId, UUID owner, String name, String species) {
        return new PetsServicePort.PetInfo(petId, owner, name, species, "Criollo", "MALE",
                LocalDate.of(2021, 5, 10), null, null, "CLAIMED");
    }

    private void petsAreOwnedByCaller() {
        when(petsServicePort.getPets(anyList())).thenReturn(Map.of(
                maxId, pet(maxId, userId, "Max", "DOG"),
                lunaId, pet(lunaId, userId, "Luna", "CAT")));
    }

    private void echoSave() {
        when(patientRepositoryPort.save(any(VeterinaryPatient.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void clinicHasOwner() {
        when(employeeRepositoryPort.findByVeterinaryIdAndRoleAndActiveTrue(veterinaryId, EmployeeRole.OWNER))
                .thenReturn(Optional.of(VeterinaryEmployee.builder()
                        .id(UUID.randomUUID())
                        .veterinaryId(veterinaryId)
                        .userId(ownerUserId)
                        .role(EmployeeRole.OWNER)
                        .active(true)
                        .build()));
    }

    private SharePetsWithVeterinaryRequest requestOf(UUID... petIds) {
        return SharePetsWithVeterinaryRequest.builder().petIds(Arrays.asList(petIds)).build();
    }

    @Test
    void share_shouldGrantConsentForEveryPet() {
        clinicExists();
        userIsLinked();
        petsAreOwnedByCaller();
        when(patientRepositoryPort.findByVeterinaryIdAndPetIdIn(veterinaryId, List.of(maxId, lunaId)))
                .thenReturn(List.of());
        echoSave();
        clinicHasOwner();

        SharePetsWithVeterinaryResponse response =
                sharePetsWithVeterinaryService.share(veterinaryId, requestOf(maxId, lunaId));

        assertThat(response.getVeterinaryId()).isEqualTo(veterinaryId);
        assertThat(response.getVeterinaryName()).isEqualTo("Clínica El Bosque");
        assertThat(response.getSharedCount()).isEqualTo(2);
        assertThat(response.getPets())
                .extracting(SharePetsWithVeterinaryResponse.SharedPet::getPetName,
                        SharePetsWithVeterinaryResponse.SharedPet::isAlreadyShared)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("Max", false),
                        org.assertj.core.groups.Tuple.tuple("Luna", false));

        ArgumentCaptor<VeterinaryPatient> saved = ArgumentCaptor.forClass(VeterinaryPatient.class);
        verify(patientRepositoryPort, times(2)).save(saved.capture());
        assertThat(saved.getAllValues()).allSatisfy(patient -> {
            assertThat(patient.getConsentStatus()).isEqualTo(ConsentStatus.GRANTED);
            assertThat(patient.getGrantedBy()).isEqualTo(userId);
            assertThat(patient.getVeterinaryId()).isEqualTo(veterinaryId);
            assertThat(patient.getRevokedAt()).isNull();
            assertThat(patient.getOrigin()).isEqualTo(PatientOrigin.SHARED_BY_OWNER);
        });
    }

    @Test
    void share_shouldValidateOwnershipWithASingleBatchCall() {
        clinicExists();
        userIsLinked();
        petsAreOwnedByCaller();
        when(patientRepositoryPort.findByVeterinaryIdAndPetIdIn(any(), anyList())).thenReturn(List.of());
        echoSave();
        clinicHasOwner();

        sharePetsWithVeterinaryService.share(veterinaryId, requestOf(maxId, lunaId));

        // Una sola llamada a Pets para N mascotas: el batch ya trae el ownerId de cada una, así que
        // no hace falta un isOwner por mascota.
        verify(petsServicePort, times(1)).getPets(List.of(maxId, lunaId));
        verify(petsServicePort, never()).isOwner(any(), any());
    }

    @Test
    void share_shouldStoreThePetNameSnapshotFromPets() {
        clinicExists();
        userIsLinked();
        petsAreOwnedByCaller();
        when(patientRepositoryPort.findByVeterinaryIdAndPetIdIn(any(), anyList())).thenReturn(List.of());
        echoSave();
        clinicHasOwner();

        sharePetsWithVeterinaryService.share(veterinaryId, requestOf(maxId));

        ArgumentCaptor<VeterinaryPatient> saved = ArgumentCaptor.forClass(VeterinaryPatient.class);
        verify(patientRepositoryPort).save(saved.capture());
        assertThat(saved.getValue().getPetName()).isEqualTo("Max");
        assertThat(saved.getValue().getSpecies()).isEqualTo("DOG");
    }

    @Test
    void share_shouldShareNothing_whenAnyPetBelongsToSomeoneElse() {
        clinicExists();
        userIsLinked();
        when(petsServicePort.getPets(anyList())).thenReturn(Map.of(
                maxId, pet(maxId, userId, "Max", "DOG"),
                lunaId, pet(lunaId, UUID.randomUUID(), "Luna", "CAT")));

        assertThatThrownBy(() ->
                sharePetsWithVeterinaryService.share(veterinaryId, requestOf(maxId, lunaId)))
                .isInstanceOf(PetNotOwnedException.class);

        // Todo o nada: ni siquiera se comparte la que sí era del caller.
        verify(patientRepositoryPort, never()).save(any());
        verify(eventPublisherPort, never()).publish(any());
    }

    @Test
    void share_shouldShareNothing_whenAPetDoesNotExist() {
        clinicExists();
        userIsLinked();
        // Pets no devuelve la mascota desconocida: se trata igual que una de otra persona, con el
        // mismo 403, para no poder enumerar mascotas probando UUIDs.
        when(petsServicePort.getPets(anyList())).thenReturn(Map.of(
                maxId, pet(maxId, userId, "Max", "DOG")));

        assertThatThrownBy(() ->
                sharePetsWithVeterinaryService.share(veterinaryId, requestOf(maxId, lunaId)))
                .isInstanceOf(PetNotOwnedException.class);

        verify(patientRepositoryPort, never()).save(any());
    }

    @Test
    void share_shouldReactivateARevokedConsent_keepingTheSameRowAndOrigin() {
        clinicExists();
        userIsLinked();
        when(petsServicePort.getPets(anyList())).thenReturn(Map.of(maxId, pet(maxId, userId, "Max", "DOG")));
        UUID existingId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now().minusMonths(6);
        when(patientRepositoryPort.findByVeterinaryIdAndPetIdIn(veterinaryId, List.of(maxId)))
                .thenReturn(List.of(VeterinaryPatient.builder()
                        .id(existingId)
                        .veterinaryId(veterinaryId)
                        .petId(maxId)
                        .origin(PatientOrigin.REGISTERED_BY_CLINIC)
                        .consentStatus(ConsentStatus.REVOKED)
                        .grantedBy(userId)
                        .grantedAt(createdAt)
                        .revokedAt(LocalDateTime.now().minusMonths(1))
                        .createdAt(createdAt)
                        .petName("Max")
                        .build()));
        echoSave();
        clinicHasOwner();

        SharePetsWithVeterinaryResponse response =
                sharePetsWithVeterinaryService.share(veterinaryId, requestOf(maxId));

        ArgumentCaptor<VeterinaryPatient> saved = ArgumentCaptor.forClass(VeterinaryPatient.class);
        verify(patientRepositoryPort).save(saved.capture());
        assertThat(saved.getValue().getId()).isEqualTo(existingId);
        assertThat(saved.getValue().getConsentStatus()).isEqualTo(ConsentStatus.GRANTED);
        assertThat(saved.getValue().getRevokedAt()).isNull();
        assertThat(saved.getValue().getGrantedAt()).isAfter(createdAt);
        // origin se conserva: una mascota que registró la clínica y el dueño después reclamó y
        // comparte sigue siendo REGISTERED_BY_CLINIC (importa para la Fase 4B).
        assertThat(saved.getValue().getOrigin()).isEqualTo(PatientOrigin.REGISTERED_BY_CLINIC);
        assertThat(saved.getValue().getCreatedAt()).isEqualTo(createdAt);
        assertThat(response.getSharedCount()).isEqualTo(1);
        assertThat(response.getPets().get(0).isAlreadyShared()).isFalse();
    }

    @Test
    void share_shouldBeIdempotent_whenThePetIsAlreadyShared() {
        clinicExists();
        userIsLinked();
        when(petsServicePort.getPets(anyList())).thenReturn(Map.of(maxId, pet(maxId, userId, "Max", "DOG")));
        LocalDateTime grantedAt = LocalDateTime.now().minusMonths(2);
        when(patientRepositoryPort.findByVeterinaryIdAndPetIdIn(veterinaryId, List.of(maxId)))
                .thenReturn(List.of(VeterinaryPatient.builder()
                        .id(UUID.randomUUID())
                        .veterinaryId(veterinaryId)
                        .petId(maxId)
                        .consentStatus(ConsentStatus.GRANTED)
                        .grantedBy(userId)
                        .grantedAt(grantedAt)
                        .createdAt(grantedAt)
                        .petName("Max")
                        .build()));
        echoSave();

        SharePetsWithVeterinaryResponse response =
                sharePetsWithVeterinaryService.share(veterinaryId, requestOf(maxId));

        assertThat(response.getSharedCount()).isZero();
        assertThat(response.getPets().get(0).isAlreadyShared()).isTrue();
        // El consentimiento no se renovó: conserva su grantedAt original.
        assertThat(response.getPets().get(0).getGrantedAt()).isEqualTo(grantedAt);
        // Y no se vuelve a notificar a la clínica por algo que ya tenía.
        verify(eventPublisherPort, never()).publish(any());
    }

    @Test
    void share_shouldPublishOneEventPerNewlySharedPet_toTheClinicOwner() {
        clinicExists();
        userIsLinked();
        petsAreOwnedByCaller();
        when(patientRepositoryPort.findByVeterinaryIdAndPetIdIn(any(), anyList())).thenReturn(List.of());
        echoSave();
        clinicHasOwner();

        sharePetsWithVeterinaryService.share(veterinaryId, requestOf(maxId, lunaId));

        ArgumentCaptor<DomainEvent> events = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisherPort, times(2)).publish(events.capture());
        assertThat(events.getAllValues()).allSatisfy(event -> {
            assertThat(event.eventType()).isEqualTo(VeterinaryEventType.PET_SHARED_WITH_VETERINARY);
            assertThat(event.userId()).isEqualTo(ownerUserId);
            assertThat(event.payload()).containsEntry("veterinaryName", "Clínica El Bosque")
                    .containsEntry("clientUserId", userId);
            assertThat(event.metadata().veterinaryId()).isEqualTo(veterinaryId);
        });
        assertThat(events.getAllValues()).extracting(event -> event.payload().get("petName"))
                .containsExactly("Max", "Luna");
    }

    @Test
    void share_shouldStillWork_whenClinicHasNoActiveOwnerToNotify() {
        clinicExists();
        userIsLinked();
        when(petsServicePort.getPets(anyList())).thenReturn(Map.of(maxId, pet(maxId, userId, "Max", "DOG")));
        when(patientRepositoryPort.findByVeterinaryIdAndPetIdIn(any(), anyList())).thenReturn(List.of());
        echoSave();
        when(employeeRepositoryPort.findByVeterinaryIdAndRoleAndActiveTrue(veterinaryId, EmployeeRole.OWNER))
                .thenReturn(Optional.empty());

        assertThat(sharePetsWithVeterinaryService.share(veterinaryId, requestOf(maxId)).getSharedCount())
                .isEqualTo(1);

        verify(eventPublisherPort, never()).publish(any());
    }

    @Test
    void share_shouldThrowNotLinked_whenUserIsNotLinkedToTheClinic() {
        clinicExists();
        when(linkRepositoryPort.findActiveLink(userId, veterinaryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sharePetsWithVeterinaryService.share(veterinaryId, requestOf(maxId)))
                .isInstanceOf(NotLinkedToVeterinaryException.class);

        // No llega a preguntarle a Pets: el requisito previo falla antes.
        verify(petsServicePort, never()).getPets(anyList());
        verify(patientRepositoryPort, never()).save(any());
    }

    @Test
    void share_shouldThrowClinicNotActive_whenClinicIsSuspended() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(
                clinic.toBuilder().status(VeterinaryStatus.SUSPENDED).active(false).build()));

        assertThatThrownBy(() -> sharePetsWithVeterinaryService.share(veterinaryId, requestOf(maxId)))
                .isInstanceOf(ClinicNotActiveException.class);

        verify(patientRepositoryPort, never()).save(any());
    }

    @Test
    void share_shouldThrowVeterinaryNotFound_whenClinicDoesNotExist() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sharePetsWithVeterinaryService.share(veterinaryId, requestOf(maxId)))
                .isInstanceOf(VeterinaryNotFoundException.class);
    }

    @Test
    void share_shouldThrowValidation_whenPetIdsIsEmptyOrNull() {
        assertThatThrownBy(() -> sharePetsWithVeterinaryService.share(veterinaryId,
                SharePetsWithVeterinaryRequest.builder().petIds(List.of()).build()))
                .isInstanceOf(InvalidPetShareException.class);

        assertThatThrownBy(() -> sharePetsWithVeterinaryService.share(veterinaryId,
                SharePetsWithVeterinaryRequest.builder().petIds(null).build()))
                .isInstanceOf(InvalidPetShareException.class);

        verify(veterinaryRepositoryPort, never()).findById(any());
    }

    @Test
    void share_shouldThrowValidation_whenPetIdsContainsNull() {
        assertThatThrownBy(() -> sharePetsWithVeterinaryService.share(veterinaryId,
                requestOf(maxId, null)))
                .isInstanceOf(InvalidPetShareException.class);
    }

    @Test
    void share_shouldDeduplicateRepeatedPetIds() {
        clinicExists();
        userIsLinked();
        when(petsServicePort.getPets(List.of(maxId)))
                .thenReturn(Map.of(maxId, pet(maxId, userId, "Max", "DOG")));
        when(patientRepositoryPort.findByVeterinaryIdAndPetIdIn(veterinaryId, List.of(maxId)))
                .thenReturn(List.of());
        echoSave();
        clinicHasOwner();

        SharePetsWithVeterinaryResponse response =
                sharePetsWithVeterinaryService.share(veterinaryId, requestOf(maxId, maxId));

        assertThat(response.getPets()).hasSize(1);
        verify(patientRepositoryPort, times(1)).save(any());
    }

    @Test
    void share_shouldPropagate503_whenPetsIsDown() {
        clinicExists();
        userIsLinked();
        when(petsServicePort.getPets(anyList()))
                .thenThrow(new ExternalServiceUnavailableException("Pets service lookup failed"));

        // Sin Pets no se puede verificar la propiedad: cortar con 503 es lo correcto, nunca
        // compartir "por si acaso".
        assertThatThrownBy(() -> sharePetsWithVeterinaryService.share(veterinaryId, requestOf(maxId)))
                .isInstanceOf(ExternalServiceUnavailableException.class);

        verify(patientRepositoryPort, never()).save(any());
    }

    @Test
    void share_shouldUseTheUserFromTheToken() {
        clinicExists();
        userIsLinked();
        when(petsServicePort.getPets(anyList())).thenReturn(Map.of(maxId, pet(maxId, userId, "Max", "DOG")));
        when(patientRepositoryPort.findByVeterinaryIdAndPetIdIn(any(), anyList())).thenReturn(List.of());
        echoSave();
        clinicHasOwner();

        sharePetsWithVeterinaryService.share(veterinaryId, requestOf(maxId));

        verify(linkRepositoryPort, times(1)).findActiveLink(userId, veterinaryId);
        verify(petsServicePort, times(1)).getPets(List.of(maxId));
    }
}

package com.MyAnimaLog.Veterinary.application.patients.services;

import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.patients.dto.RevokePetShareResponse;
import com.MyAnimaLog.Veterinary.application.patients.ports.out.VeterinaryPatientRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.dto.AuthenticatedUser;
import com.MyAnimaLog.Veterinary.application.shared.dto.DomainEvent;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.AuthenticatedUserPort;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.EventPublisherPort;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.PetsServicePort;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.patients.enums.ConsentStatus;
import com.MyAnimaLog.Veterinary.domain.patients.exceptions.PetNotOwnedException;
import com.MyAnimaLog.Veterinary.domain.patients.model.VeterinaryPatient;
import com.MyAnimaLog.Veterinary.domain.shared.enums.VeterinaryEventType;
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

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RevokePetShareServiceTest {

    @Mock
    private VeterinaryRepositoryPort veterinaryRepositoryPort;

    @Mock
    private VeterinaryPatientRepositoryPort patientRepositoryPort;

    @Mock
    private VeterinaryEmployeeRepositoryPort employeeRepositoryPort;

    @Mock
    private AuthenticatedUserPort authenticatedUserPort;

    @Mock
    private EventPublisherPort eventPublisherPort;

    @InjectMocks
    private RevokePetShareService revokePetShareService;

    private UUID userId;
    private UUID veterinaryId;
    private UUID ownerUserId;
    private UUID petId;
    private Veterinary clinic;
    private VeterinaryPatient grantedShare;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        veterinaryId = UUID.randomUUID();
        ownerUserId = UUID.randomUUID();
        petId = UUID.randomUUID();

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

        grantedShare = VeterinaryPatient.builder()
                .id(UUID.randomUUID())
                .veterinaryId(veterinaryId)
                .petId(petId)
                .consentStatus(ConsentStatus.GRANTED)
                .grantedBy(userId)
                .grantedAt(LocalDateTime.now().minusMonths(1))
                .createdAt(LocalDateTime.now().minusMonths(1))
                .petName("Max")
                .species("DOG")
                .build();
    }

    private void clinicExists() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(clinic));
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

    @Test
    void revoke_shouldMarkTheConsentRevoked_withoutDeletingTheRow() {
        clinicExists();
        when(patientRepositoryPort.findByVeterinaryIdAndPetId(veterinaryId, petId))
                .thenReturn(Optional.of(grantedShare));
        echoSave();
        clinicHasOwner();

        RevokePetShareResponse response = revokePetShareService.revoke(veterinaryId, petId);

        ArgumentCaptor<VeterinaryPatient> saved = ArgumentCaptor.forClass(VeterinaryPatient.class);
        verify(patientRepositoryPort).save(saved.capture());
        assertThat(saved.getValue().getId()).isEqualTo(grantedShare.getId());
        assertThat(saved.getValue().getConsentStatus()).isEqualTo(ConsentStatus.REVOKED);
        assertThat(saved.getValue().getRevokedAt()).isNotNull();

        assertThat(response.getPetName()).isEqualTo("Max");
        assertThat(response.getRevokedAt()).isNotNull();
        assertThat(response.isAlreadyRevoked()).isFalse();
    }

    /**
     * Revocar <strong>no depende de Pets</strong>: la autorización sale de {@code granted_by}, que
     * es nuestro propio registro de quién dio el consentimiento. Si dependiera de un {@code isOwner}
     * remoto, una caída de Pets dejaría al dueño sin poder cortar el acceso a su historia clínica —
     * el peor momento para no poder. Se verifica estructuralmente para que agregar la dependencia
     * rompa el test en vez de pasar inadvertido.
     */
    @Test
    void revoke_shouldNotDependOnPetsService() {
        assertThat(RevokePetShareService.class.getDeclaredFields())
                .noneMatch(field -> PetsServicePort.class.equals(field.getType()));
    }

    @Test
    void revoke_shouldBeIdempotent_whenTheConsentWasAlreadyRevoked() {
        clinicExists();
        LocalDateTime revokedAt = LocalDateTime.now().minusDays(3);
        when(patientRepositoryPort.findByVeterinaryIdAndPetId(veterinaryId, petId))
                .thenReturn(Optional.of(grantedShare.toBuilder()
                        .consentStatus(ConsentStatus.REVOKED)
                        .revokedAt(revokedAt)
                        .build()));

        RevokePetShareResponse response = revokePetShareService.revoke(veterinaryId, petId);

        assertThat(response.isAlreadyRevoked()).isTrue();
        assertThat(response.getRevokedAt()).isEqualTo(revokedAt);
        verify(patientRepositoryPort, never()).save(any());
        verify(eventPublisherPort, never()).publish(any());
    }

    @Test
    void revoke_shouldPublishPetShareRevoked_toTheClinicOwner() {
        clinicExists();
        when(patientRepositoryPort.findByVeterinaryIdAndPetId(veterinaryId, petId))
                .thenReturn(Optional.of(grantedShare));
        echoSave();
        clinicHasOwner();

        revokePetShareService.revoke(veterinaryId, petId);

        ArgumentCaptor<DomainEvent> event = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisherPort).publish(event.capture());
        assertThat(event.getValue().eventType()).isEqualTo(VeterinaryEventType.PET_SHARE_REVOKED);
        assertThat(event.getValue().userId()).isEqualTo(ownerUserId);
        assertThat(event.getValue().payload())
                .containsEntry("veterinaryName", "Clínica El Bosque")
                .containsEntry("petId", petId)
                .containsEntry("petName", "Max")
                .containsEntry("clientUserId", userId);
    }

    @Test
    void revoke_shouldThrowPetNotOwned_whenTheShareWasGrantedBySomeoneElse() {
        clinicExists();
        when(patientRepositoryPort.findByVeterinaryIdAndPetId(veterinaryId, petId))
                .thenReturn(Optional.of(grantedShare.toBuilder().grantedBy(UUID.randomUUID()).build()));

        assertThatThrownBy(() -> revokePetShareService.revoke(veterinaryId, petId))
                .isInstanceOf(PetNotOwnedException.class);

        verify(patientRepositoryPort, never()).save(any());
    }

    @Test
    void revoke_shouldThrowPetNotOwned_whenThereIsNoShareAtAll() {
        clinicExists();
        when(patientRepositoryPort.findByVeterinaryIdAndPetId(veterinaryId, petId))
                .thenReturn(Optional.empty());

        // Mismo error que "la compartió otra persona": distinguirlos revelaría si esa mascota está
        // compartida con esa clínica, que no es información del caller.
        assertThatThrownBy(() -> revokePetShareService.revoke(veterinaryId, petId))
                .isInstanceOf(PetNotOwnedException.class);
    }

    @Test
    void revoke_shouldWorkEvenWhenTheClinicIsSuspended() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(
                clinic.toBuilder().status(VeterinaryStatus.SUSPENDED).active(false).build()));
        when(patientRepositoryPort.findByVeterinaryIdAndPetId(veterinaryId, petId))
                .thenReturn(Optional.of(grantedShare));
        echoSave();
        clinicHasOwner();

        // Cortar el acceso tiene que poder hacerse siempre, sin importar el estado de la clínica.
        assertThat(revokePetShareService.revoke(veterinaryId, petId).isAlreadyRevoked()).isFalse();
    }

    @Test
    void revoke_shouldThrowVeterinaryNotFound_whenClinicDoesNotExist() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> revokePetShareService.revoke(veterinaryId, petId))
                .isInstanceOf(VeterinaryNotFoundException.class);

        verify(patientRepositoryPort, never()).save(any());
    }
}

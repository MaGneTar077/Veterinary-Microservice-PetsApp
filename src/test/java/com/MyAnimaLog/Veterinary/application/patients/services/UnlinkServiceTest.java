package com.MyAnimaLog.Veterinary.application.patients.services;

import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.patients.dto.UnlinkResponse;
import com.MyAnimaLog.Veterinary.application.patients.ports.out.UserVeterinaryLinkRepositoryPort;
import com.MyAnimaLog.Veterinary.application.patients.ports.out.VeterinaryPatientRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.dto.AuthenticatedUser;
import com.MyAnimaLog.Veterinary.application.shared.dto.DomainEvent;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.AuthenticatedUserPort;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.EventPublisherPort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.patients.enums.ConsentStatus;
import com.MyAnimaLog.Veterinary.domain.patients.enums.LinkStatus;
import com.MyAnimaLog.Veterinary.domain.patients.exceptions.UserNotLinkedException;
import com.MyAnimaLog.Veterinary.domain.patients.model.UserVeterinaryLink;
import com.MyAnimaLog.Veterinary.domain.patients.model.VeterinaryPatient;
import com.MyAnimaLog.Veterinary.domain.security.Permission;
import com.MyAnimaLog.Veterinary.domain.shared.enums.VeterinaryEventType;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.InsufficientPermissionException;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UnlinkServiceTest {

    @Mock
    private VeterinaryRepositoryPort veterinaryRepositoryPort;

    @Mock
    private UserVeterinaryLinkRepositoryPort linkRepositoryPort;

    @Mock
    private VeterinaryPatientRepositoryPort patientRepositoryPort;

    @Mock
    private VeterinaryEmployeeRepositoryPort employeeRepositoryPort;

    @Mock
    private VeterinaryAuthorizationService authorizationService;

    @Mock
    private AuthenticatedUserPort authenticatedUserPort;

    @Mock
    private EventPublisherPort eventPublisherPort;

    @InjectMocks
    private UnlinkService unlinkService;

    private UUID userId;
    private UUID veterinaryId;
    private UUID ownerUserId;
    private Veterinary veterinary;
    private UserVeterinaryLink existingLink;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        veterinaryId = UUID.randomUUID();
        ownerUserId = UUID.randomUUID();

        veterinary = Veterinary.builder()
                .id(veterinaryId)
                .name("Clínica El Bosque")
                .city("Cartagena")
                .tenantId(UUID.randomUUID().toString())
                .status(VeterinaryStatus.ACTIVE)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        existingLink = UserVeterinaryLink.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .veterinaryId(veterinaryId)
                .status(LinkStatus.LINKED)
                .linkedAt(LocalDateTime.now().minusMonths(1))
                .ownerName("Ana Pérez")
                .ownerEmail("ana@correo.com")
                .ownerPhone("+573001234567")
                .build();
    }

    private void linkExists() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(veterinary));
        when(linkRepositoryPort.findActiveLink(userId, veterinaryId)).thenReturn(Optional.of(existingLink));
    }

    private void callerIsTheClient() {
        when(authenticatedUserPort.current()).thenReturn(
                new AuthenticatedUser(userId, "ana@correo.com", true, false, Optional.empty()));
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

    private VeterinaryPatient sharedPet(String name) {
        return VeterinaryPatient.builder()
                .id(UUID.randomUUID())
                .veterinaryId(veterinaryId)
                .petId(UUID.randomUUID())
                .consentStatus(ConsentStatus.GRANTED)
                .grantedBy(userId)
                .grantedAt(LocalDateTime.now().minusDays(5))
                .petName(name)
                .build();
    }

    @Test
    void unlink_shouldReturnResponse_whenLinkExists() {
        linkExists();
        when(patientRepositoryPort.findGrantedByVeterinaryIdAndGrantedBy(veterinaryId, userId))
                .thenReturn(List.of());
        callerIsTheClient();
        clinicHasOwner();

        UnlinkResponse response = unlinkService.unlink(userId, veterinaryId);

        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getVeterinaryId()).isEqualTo(veterinaryId);
        assertThat(response.getRevokedPetsCount()).isZero();
        assertThat(response.getMessage()).isEqualTo("User unlinked successfully");
    }

    @Test
    void unlink_shouldSoftUnlink_insteadOfDeletingTheRow() {
        linkExists();
        when(patientRepositoryPort.findGrantedByVeterinaryIdAndGrantedBy(veterinaryId, userId))
                .thenReturn(List.of());
        callerIsTheClient();
        clinicHasOwner();

        unlinkService.unlink(userId, veterinaryId);

        ArgumentCaptor<UserVeterinaryLink> saved = ArgumentCaptor.forClass(UserVeterinaryLink.class);
        verify(linkRepositoryPort).save(saved.capture());
        assertThat(saved.getValue().getId()).isEqualTo(existingLink.getId());
        assertThat(saved.getValue().getStatus()).isEqualTo(LinkStatus.UNLINKED);
        assertThat(saved.getValue().getUnlinkedAt()).isNotNull();
    }

    @Test
    void unlink_shouldWipeEmailAndPhoneFromTheSnapshot_butKeepTheName() {
        linkExists();
        when(patientRepositoryPort.findGrantedByVeterinaryIdAndGrantedBy(veterinaryId, userId))
                .thenReturn(List.of());
        callerIsTheClient();
        clinicHasOwner();

        unlinkService.unlink(userId, veterinaryId);

        // Ley 1581 de 2012: la clínica ya no tiene razón para guardar el contacto de alguien que
        // cortó la relación, pero sin el nombre el histórico sería ilegible.
        ArgumentCaptor<UserVeterinaryLink> saved = ArgumentCaptor.forClass(UserVeterinaryLink.class);
        verify(linkRepositoryPort).save(saved.capture());
        assertThat(saved.getValue().getOwnerEmail()).isNull();
        assertThat(saved.getValue().getOwnerPhone()).isNull();
        assertThat(saved.getValue().getOwnerName()).isEqualTo("Ana Pérez");
    }

    @Test
    void unlink_shouldRevokeEverySharedPet_inCascade() {
        linkExists();
        VeterinaryPatient max = sharedPet("Max");
        VeterinaryPatient luna = sharedPet("Luna");
        when(patientRepositoryPort.findGrantedByVeterinaryIdAndGrantedBy(veterinaryId, userId))
                .thenReturn(List.of(max, luna));
        callerIsTheClient();
        clinicHasOwner();

        UnlinkResponse response = unlinkService.unlink(userId, veterinaryId);

        // Lo más importante de la desvinculación: si no se revocaran, la clínica seguiría teniendo
        // acceso por el endpoint `access`, que mira veterinary_patients y no la vinculación.
        ArgumentCaptor<VeterinaryPatient> revoked = ArgumentCaptor.forClass(VeterinaryPatient.class);
        verify(patientRepositoryPort, times(2)).save(revoked.capture());
        assertThat(revoked.getAllValues())
                .allSatisfy(patient -> {
                    assertThat(patient.getConsentStatus()).isEqualTo(ConsentStatus.REVOKED);
                    assertThat(patient.getRevokedAt()).isNotNull();
                })
                .extracting(VeterinaryPatient::getPetName)
                .containsExactlyInAnyOrder("Max", "Luna");
        assertThat(response.getRevokedPetsCount()).isEqualTo(2);
    }

    @Test
    void unlink_shouldPublishUserUnlinkedEvent_toTheClinicOwner() {
        linkExists();
        when(patientRepositoryPort.findGrantedByVeterinaryIdAndGrantedBy(veterinaryId, userId))
                .thenReturn(List.of(sharedPet("Max")));
        callerIsTheClient();
        clinicHasOwner();

        unlinkService.unlink(userId, veterinaryId);

        ArgumentCaptor<DomainEvent> event = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisherPort).publish(event.capture());
        assertThat(event.getValue().eventType()).isEqualTo(VeterinaryEventType.USER_UNLINKED);
        assertThat(event.getValue().userId()).isEqualTo(ownerUserId);
        assertThat(event.getValue().payload())
                .containsEntry("clientUserId", userId)
                .containsEntry("clientName", "Ana Pérez")
                .containsEntry("revokedPetsCount", 1)
                .containsEntry("initiatedByClient", true);
    }

    @Test
    void unlink_shouldFlagInitiatedByClientFalse_whenClinicStaffRemovesTheClient() {
        linkExists();
        when(patientRepositoryPort.findGrantedByVeterinaryIdAndGrantedBy(veterinaryId, userId))
                .thenReturn(List.of());
        when(authenticatedUserPort.current()).thenReturn(
                new AuthenticatedUser(UUID.randomUUID(), "admin@clinica.com", true, false, Optional.empty()));
        clinicHasOwner();

        unlinkService.unlink(userId, veterinaryId);

        ArgumentCaptor<DomainEvent> event = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisherPort).publish(event.capture());
        assertThat(event.getValue().payload()).containsEntry("initiatedByClient", false);
    }

    @Test
    void unlink_shouldThrowVeterinaryNotFoundException_whenVeterinaryDoesNotExist() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> unlinkService.unlink(userId, veterinaryId))
                .isInstanceOf(VeterinaryNotFoundException.class);

        verify(linkRepositoryPort, never()).save(any());
        verify(patientRepositoryPort, never()).save(any());
    }

    @Test
    void unlink_shouldThrowUserNotLinkedException_whenThereIsNoActiveLink() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(veterinary));
        when(linkRepositoryPort.findActiveLink(userId, veterinaryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> unlinkService.unlink(userId, veterinaryId))
                .isInstanceOf(UserNotLinkedException.class);

        verify(linkRepositoryPort, never()).save(any());
        verify(patientRepositoryPort, never()).save(any());
    }

    @Test
    void unlink_shouldReturn403_whenCallerIsNeitherSelfNorAuthorizedStaff() {
        doThrow(new InsufficientPermissionException(Permission.CLINIC_CONFIGURE))
                .when(authorizationService)
                .requireSelfOrPermission(userId, veterinaryId, Permission.CLINIC_CONFIGURE);

        assertThatThrownBy(() -> unlinkService.unlink(userId, veterinaryId))
                .isInstanceOf(InsufficientPermissionException.class);

        verify(linkRepositoryPort, never()).save(any());
        verify(patientRepositoryPort, never()).save(any());
    }

    @Test
    void unlink_shouldCheckSelfOrPermission_beforeTouchingRepositories() {
        linkExists();
        when(patientRepositoryPort.findGrantedByVeterinaryIdAndGrantedBy(veterinaryId, userId))
                .thenReturn(List.of());
        callerIsTheClient();
        clinicHasOwner();

        unlinkService.unlink(userId, veterinaryId);

        verify(authorizationService, times(1))
                .requireSelfOrPermission(userId, veterinaryId, Permission.CLINIC_CONFIGURE);
    }
}

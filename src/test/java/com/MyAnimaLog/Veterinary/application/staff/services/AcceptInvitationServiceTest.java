package com.MyAnimaLog.Veterinary.application.staff.services;

import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.professional.ports.out.ProfessionalProfileRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.dto.AuthenticatedUser;
import com.MyAnimaLog.Veterinary.application.shared.dto.DomainEvent;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.AuthenticatedUserPort;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.EventPublisherPort;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.UserServicePort;
import com.MyAnimaLog.Veterinary.application.staff.dto.AcceptInvitationRequest;
import com.MyAnimaLog.Veterinary.application.staff.dto.AcceptInvitationResponse;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.EmployeeInvitationRepositoryPort;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.ClinicNotActiveException;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.EmailNotVerifiedException;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.professional.enums.IdDocumentType;
import com.MyAnimaLog.Veterinary.domain.professional.enums.LicenseStatus;
import com.MyAnimaLog.Veterinary.domain.professional.enums.Profession;
import com.MyAnimaLog.Veterinary.domain.professional.model.ProfessionalProfile;
import com.MyAnimaLog.Veterinary.domain.shared.enums.VeterinaryEventType;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.domain.staff.enums.InvitationStatus;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.EmployeeAlreadyExistsException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.InvitationEmailMismatchException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.InvitationExpiredException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.InvitationNotFoundException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.InvitationNotPendingException;
import com.MyAnimaLog.Veterinary.domain.staff.model.EmployeeInvitation;
import com.MyAnimaLog.Veterinary.domain.staff.model.VeterinaryEmployee;
import com.MyAnimaLog.Veterinary.infrastructure.security.InvitationTokens;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AcceptInvitationServiceTest {

    private static final String TOKEN = "token-de-prueba";

    @Mock
    private EmployeeInvitationRepositoryPort invitationRepositoryPort;

    @Mock
    private VeterinaryEmployeeRepositoryPort employeeRepositoryPort;

    @Mock
    private VeterinaryRepositoryPort veterinaryRepositoryPort;

    @Mock
    private ProfessionalProfileRepositoryPort professionalProfileRepositoryPort;

    @Mock
    private AuthenticatedUserPort authenticatedUserPort;

    @Mock
    private UserServicePort userServicePort;

    @Mock
    private EventPublisherPort eventPublisherPort;

    @InjectMocks
    private AcceptInvitationService acceptInvitationService;

    private UUID veterinaryId;
    private UUID inviteeUserId;
    private UUID ownerUserId;
    private Veterinary activeClinic;
    private EmployeeInvitation invitation;
    private AcceptInvitationRequest request;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();
        inviteeUserId = UUID.randomUUID();
        ownerUserId = UUID.randomUUID();

        activeClinic = Veterinary.builder()
                .id(veterinaryId)
                .name("Clínica El Bosque")
                .status(VeterinaryStatus.ACTIVE)
                .active(true)
                .build();

        invitation = EmployeeInvitation.builder()
                .id(UUID.randomUUID())
                .veterinaryId(veterinaryId)
                .email("invitado@clinica.com")
                .role(EmployeeRole.VETERINARIAN)
                .tokenHash(InvitationTokens.hash(TOKEN))
                .status(InvitationStatus.PENDING)
                .invitedBy(ownerUserId)
                .expiresAt(LocalDateTime.now().plusDays(3))
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();

        request = AcceptInvitationRequest.builder().token(TOKEN).build();

        when(authenticatedUserPort.current()).thenReturn(
                new AuthenticatedUser(inviteeUserId, "invitado@clinica.com", true, false, Optional.empty()));
        when(invitationRepositoryPort.findByTokenHash(InvitationTokens.hash(TOKEN)))
                .thenReturn(Optional.of(invitation));
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(activeClinic));
        when(employeeRepositoryPort.findByVeterinaryIdAndUserId(veterinaryId, inviteeUserId))
                .thenReturn(Optional.empty());
        when(employeeRepositoryPort.findByVeterinaryIdAndRoleAndActiveTrue(veterinaryId, EmployeeRole.OWNER))
                .thenReturn(Optional.of(VeterinaryEmployee.builder()
                        .id(UUID.randomUUID())
                        .veterinaryId(veterinaryId)
                        .userId(ownerUserId)
                        .role(EmployeeRole.OWNER)
                        .active(true)
                        .build()));
        when(employeeRepositoryPort.save(any(VeterinaryEmployee.class)))
                .thenAnswer(call -> call.getArgument(0));
        when(invitationRepositoryPort.save(any(EmployeeInvitation.class)))
                .thenAnswer(call -> call.getArgument(0));
        when(professionalProfileRepositoryPort.findByUserId(inviteeUserId)).thenReturn(Optional.empty());
        when(userServicePort.getUsers(anyList())).thenReturn(Map.of());
    }

    @Test
    void accept_shouldCreateAnActiveEmployeeWithTheInvitedRole() {
        AcceptInvitationResponse response = acceptInvitationService.accept(request);

        ArgumentCaptor<VeterinaryEmployee> captor = ArgumentCaptor.forClass(VeterinaryEmployee.class);
        verify(employeeRepositoryPort).save(captor.capture());
        VeterinaryEmployee created = captor.getValue();
        assertThat(created.getVeterinaryId()).isEqualTo(veterinaryId);
        assertThat(created.getUserId()).isEqualTo(inviteeUserId);
        assertThat(created.getRole()).isEqualTo(EmployeeRole.VETERINARIAN);
        assertThat(created.getActive()).isTrue();
        assertThat(created.getRemovedAt()).isNull();
        assertThat(response.getRole()).isEqualTo(EmployeeRole.VETERINARIAN);
        assertThat(response.getVeterinaryName()).isEqualTo("Clínica El Bosque");
        assertThat(response.isReactivated()).isFalse();
    }

    @Test
    void accept_shouldMarkTheInvitationAsAcceptedWithTheAcceptingUser() {
        acceptInvitationService.accept(request);

        ArgumentCaptor<EmployeeInvitation> captor = ArgumentCaptor.forClass(EmployeeInvitation.class);
        verify(invitationRepositoryPort).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(InvitationStatus.ACCEPTED);
        assertThat(captor.getValue().getAcceptedUserId()).isEqualTo(inviteeUserId);
        assertThat(captor.getValue().getDecidedAt()).isNotNull();
    }

    @Test
    void accept_shouldReactivateTheExistingInactiveEmployeeInsteadOfDuplicatingIt() {
        VeterinaryEmployee previous = VeterinaryEmployee.builder()
                .id(UUID.randomUUID())
                .veterinaryId(veterinaryId)
                .userId(inviteeUserId)
                .role(EmployeeRole.ASSISTANT)
                .active(false)
                .removedAt(LocalDateTime.now().minusMonths(6))
                .createdAt(LocalDateTime.now().minusYears(1))
                .build();
        when(employeeRepositoryPort.findByVeterinaryIdAndUserId(veterinaryId, inviteeUserId))
                .thenReturn(Optional.of(previous));

        AcceptInvitationResponse response = acceptInvitationService.accept(request);

        ArgumentCaptor<VeterinaryEmployee> captor = ArgumentCaptor.forClass(VeterinaryEmployee.class);
        verify(employeeRepositoryPort).save(captor.capture());
        VeterinaryEmployee reactivated = captor.getValue();
        // Misma fila (mismo id y created_at original), rol nuevo y sin fecha de retiro.
        assertThat(reactivated.getId()).isEqualTo(previous.getId());
        assertThat(reactivated.getCreatedAt()).isEqualTo(previous.getCreatedAt());
        assertThat(reactivated.getRole()).isEqualTo(EmployeeRole.VETERINARIAN);
        assertThat(reactivated.getActive()).isTrue();
        assertThat(reactivated.getRemovedAt()).isNull();
        assertThat(response.isReactivated()).isTrue();
        assertThat(response.getEmployeeId()).isEqualTo(previous.getId());
    }

    @Test
    void accept_shouldThrowEmployeeAlreadyExistsException_whenTheUserIsAlreadyAnActiveEmployee() {
        when(employeeRepositoryPort.findByVeterinaryIdAndUserId(veterinaryId, inviteeUserId))
                .thenReturn(Optional.of(VeterinaryEmployee.builder()
                        .id(UUID.randomUUID())
                        .veterinaryId(veterinaryId)
                        .userId(inviteeUserId)
                        .role(EmployeeRole.ASSISTANT)
                        .active(true)
                        .build()));

        assertThatThrownBy(() -> acceptInvitationService.accept(request))
                .isInstanceOf(EmployeeAlreadyExistsException.class);

        verify(employeeRepositoryPort, never()).save(any());
        verify(invitationRepositoryPort, never()).save(any());
    }

    @Test
    void accept_shouldThrowInvitationEmailMismatchException_whenTheTokenEmailIsNotTheInvitedOne() {
        when(authenticatedUserPort.current()).thenReturn(
                new AuthenticatedUser(inviteeUserId, "otra@persona.com", true, false, Optional.empty()));

        assertThatThrownBy(() -> acceptInvitationService.accept(request))
                .isInstanceOf(InvitationEmailMismatchException.class);

        verify(employeeRepositoryPort, never()).save(any());
    }

    @Test
    void accept_shouldIgnoreCaseAndSurroundingSpacesWhenComparingEmails() {
        when(authenticatedUserPort.current()).thenReturn(
                new AuthenticatedUser(inviteeUserId, "  Invitado@Clinica.COM ", true, false, Optional.empty()));

        acceptInvitationService.accept(request);

        verify(employeeRepositoryPort).save(any(VeterinaryEmployee.class));
    }

    @Test
    void accept_shouldThrowEmailNotVerifiedException_whenTheCallerEmailIsNotVerified() {
        when(authenticatedUserPort.current()).thenReturn(
                new AuthenticatedUser(inviteeUserId, "invitado@clinica.com", false, false, Optional.empty()));

        assertThatThrownBy(() -> acceptInvitationService.accept(request))
                .isInstanceOf(EmailNotVerifiedException.class);

        verify(employeeRepositoryPort, never()).save(any());
    }

    @Test
    void accept_shouldThrowInvitationExpiredException_whenTheInvitationIsPastItsExpiry() {
        when(invitationRepositoryPort.findByTokenHash(InvitationTokens.hash(TOKEN)))
                .thenReturn(Optional.of(invitation.toBuilder()
                        .expiresAt(LocalDateTime.now().minusMinutes(1))
                        .build()));

        assertThatThrownBy(() -> acceptInvitationService.accept(request))
                .isInstanceOf(InvitationExpiredException.class);

        verify(employeeRepositoryPort, never()).save(any());
    }

    @Test
    void accept_shouldThrowInvitationNotPendingException_whenTheInvitationWasRevokedOrAlreadyUsed() {
        for (InvitationStatus status : new InvitationStatus[]{
                InvitationStatus.REVOKED, InvitationStatus.ACCEPTED, InvitationStatus.DECLINED}) {
            when(invitationRepositoryPort.findByTokenHash(InvitationTokens.hash(TOKEN)))
                    .thenReturn(Optional.of(invitation.toBuilder().status(status).build()));

            assertThatThrownBy(() -> acceptInvitationService.accept(request))
                    .isInstanceOf(InvitationNotPendingException.class);
        }
        verify(employeeRepositoryPort, never()).save(any());
    }

    @Test
    void accept_shouldThrowInvitationNotFoundException_whenTheTokenIsUnknownOrMissing() {
        when(invitationRepositoryPort.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> acceptInvitationService.accept(request))
                .isInstanceOf(InvitationNotFoundException.class);

        assertThatThrownBy(() -> acceptInvitationService.accept(
                AcceptInvitationRequest.builder().token(null).build()))
                .isInstanceOf(InvitationNotFoundException.class);
        assertThatThrownBy(() -> acceptInvitationService.accept(
                AcceptInvitationRequest.builder().token("   ").build()))
                .isInstanceOf(InvitationNotFoundException.class);
    }

    @Test
    void accept_shouldThrowClinicNotActiveException_whenTheClinicIsSuspendedOrNotApproved() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(
                activeClinic.toBuilder().status(VeterinaryStatus.SUSPENDED).active(false).build()));

        assertThatThrownBy(() -> acceptInvitationService.accept(request))
                .isInstanceOf(ClinicNotActiveException.class);

        verify(employeeRepositoryPort, never()).save(any());
    }

    @Test
    void accept_shouldReportLicensedFromTheProfessionalProfile() {
        when(professionalProfileRepositoryPort.findByUserId(inviteeUserId)).thenReturn(Optional.of(
                ProfessionalProfile.builder()
                        .id(UUID.randomUUID())
                        .userId(inviteeUserId)
                        .fullName("María Pérez")
                        .documentType(IdDocumentType.CC)
                        .documentNumber("123456789")
                        .licenseNumber("MVZ-1234")
                        .profession(Profession.MVZ)
                        .licenseStatus(LicenseStatus.VERIFIED)
                        .build()));

        assertThat(acceptInvitationService.accept(request).isLicensed()).isTrue();
    }

    @Test
    void accept_shouldReportNotLicensed_whenTheProfileIsNotVerifiedYet() {
        when(professionalProfileRepositoryPort.findByUserId(inviteeUserId)).thenReturn(Optional.of(
                ProfessionalProfile.builder()
                        .id(UUID.randomUUID())
                        .userId(inviteeUserId)
                        .fullName("María Pérez")
                        .documentType(IdDocumentType.CC)
                        .documentNumber("123456789")
                        .licenseNumber("MVZ-1234")
                        .profession(Profession.MVZ)
                        .licenseStatus(LicenseStatus.PENDING)
                        .build()));

        assertThat(acceptInvitationService.accept(request).isLicensed()).isFalse();
    }

    @Test
    void accept_shouldPublishEmployeeJoinedToTheActiveOwner() {
        acceptInvitationService.accept(request);

        ArgumentCaptor<DomainEvent> captor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisherPort).publish(captor.capture());
        DomainEvent event = captor.getValue();
        assertThat(event.eventType()).isEqualTo(VeterinaryEventType.EMPLOYEE_JOINED);
        assertThat(event.userId()).isEqualTo(ownerUserId);
        assertThat(event.payload()).containsEntry("role", "VETERINARIAN");
        assertThat(event.payload()).containsEntry("employeeEmail", "invitado@clinica.com");
        assertThat(event.metadata().veterinaryId()).isEqualTo(veterinaryId);
    }

    @Test
    void accept_shouldStillSucceed_whenTheClinicHasNoActiveOwnerToNotify() {
        when(employeeRepositoryPort.findByVeterinaryIdAndRoleAndActiveTrue(veterinaryId, EmployeeRole.OWNER))
                .thenReturn(Optional.empty());

        assertThat(acceptInvitationService.accept(request).getEmployeeId()).isNotNull();

        verify(eventPublisherPort, never()).publish(any());
    }

    @Test
    void accept_shouldResolveTheEmployeeNameBestEffort_withoutFailingWhenItIsUnavailable() {
        when(userServicePort.getUsers(List.of(inviteeUserId))).thenReturn(Map.of(
                inviteeUserId, new UserServicePort.UserInfo(
                        inviteeUserId, "invitado@clinica.com", true, "María Pérez")));

        acceptInvitationService.accept(request);

        ArgumentCaptor<DomainEvent> captor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisherPort).publish(captor.capture());
        assertThat(captor.getValue().payload()).containsEntry("employeeName", "María Pérez");
    }
}

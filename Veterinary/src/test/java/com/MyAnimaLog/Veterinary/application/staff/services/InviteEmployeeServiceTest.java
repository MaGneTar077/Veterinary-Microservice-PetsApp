package com.MyAnimaLog.Veterinary.application.staff.services;

import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.dto.AuthenticatedUser;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.AuthenticatedUserPort;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.UserServicePort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.application.staff.dto.InviteEmployeeRequest;
import com.MyAnimaLog.Veterinary.application.staff.dto.InviteEmployeeResponse;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.EmployeeInvitationRepositoryPort;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.ClinicNotActiveException;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.security.Permission;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.InsufficientPermissionException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VeterinaryNotFoundException;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.domain.staff.enums.InvitationStatus;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.EmployeeAlreadyExistsException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.InvalidEmployeeRoleException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.InvalidInvitationEmailException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.InvitationAlreadyPendingException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.OnlyOwnerCanManageAdminsException;
import com.MyAnimaLog.Veterinary.domain.staff.model.EmployeeInvitation;
import com.MyAnimaLog.Veterinary.domain.staff.model.VeterinaryEmployee;
import com.MyAnimaLog.Veterinary.infrastructure.security.InvitationTokens;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InviteEmployeeServiceTest {

    @Mock
    private EmployeeInvitationRepositoryPort invitationRepositoryPort;

    @Mock
    private VeterinaryEmployeeRepositoryPort employeeRepositoryPort;

    @Mock
    private VeterinaryRepositoryPort veterinaryRepositoryPort;

    @Mock
    private VeterinaryAuthorizationService authorizationService;

    @Mock
    private AuthenticatedUserPort authenticatedUserPort;

    @Mock
    private UserServicePort userServicePort;

    @Mock
    private InvitationNotifier invitationNotifier;

    @InjectMocks
    private InviteEmployeeService inviteEmployeeService;

    @Captor
    private ArgumentCaptor<EmployeeInvitation> invitationCaptor;

    private UUID veterinaryId;
    private UUID callerUserId;
    private Veterinary activeClinic;
    private VeterinaryEmployee owner;
    private InviteEmployeeRequest validRequest;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();
        callerUserId = UUID.randomUUID();

        activeClinic = Veterinary.builder()
                .id(veterinaryId)
                .name("Clínica El Bosque")
                .status(VeterinaryStatus.ACTIVE)
                .active(true)
                .build();

        owner = VeterinaryEmployee.builder()
                .id(UUID.randomUUID())
                .veterinaryId(veterinaryId)
                .userId(callerUserId)
                .role(EmployeeRole.OWNER)
                .active(true)
                .build();

        validRequest = InviteEmployeeRequest.builder()
                .email("nuevo@clinica.com")
                .role(EmployeeRole.VETERINARIAN)
                .build();

        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(activeClinic));
        when(authenticatedUserPort.current()).thenReturn(
                new AuthenticatedUser(callerUserId, "owner@clinica.com", true, false, Optional.empty()));
        when(employeeRepositoryPort.findByVeterinaryIdAndUserId(veterinaryId, callerUserId))
                .thenReturn(Optional.of(owner));
        when(employeeRepositoryPort.findByVeterinaryId(veterinaryId)).thenReturn(List.of(owner));
        when(userServicePort.getUsers(anyList())).thenReturn(Map.of());
        when(invitationRepositoryPort.findByVeterinaryIdAndEmailAndStatus(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(invitationRepositoryPort.findByVeterinaryIdAndStatus(veterinaryId, InvitationStatus.PENDING))
                .thenReturn(List.of());
        when(invitationRepositoryPort.save(any(EmployeeInvitation.class)))
                .thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void invite_shouldRequireStaffManageOverTheClinicInThePath() {
        inviteEmployeeService.invite(veterinaryId, validRequest);

        verify(authorizationService).require(veterinaryId, Permission.STAFF_MANAGE);
    }

    @Test
    void invite_shouldCreatePendingInvitationWithSevenDayExpiry() {
        LocalDateTime before = LocalDateTime.now();

        InviteEmployeeResponse response = inviteEmployeeService.invite(veterinaryId, validRequest);

        verify(invitationRepositoryPort).save(invitationCaptor.capture());
        EmployeeInvitation saved = invitationCaptor.getValue();
        assertThat(saved.getStatus()).isEqualTo(InvitationStatus.PENDING);
        assertThat(saved.getRole()).isEqualTo(EmployeeRole.VETERINARIAN);
        assertThat(saved.getInvitedBy()).isEqualTo(callerUserId);
        assertThat(saved.getExpiresAt()).isAfter(before.plusDays(6)).isBefore(before.plusDays(8));
        assertThat(response.getInvitation().getStatus()).isEqualTo(InvitationStatus.PENDING);
        assertThat(response.getInvitation().isExpired()).isFalse();
    }

    @Test
    void invite_shouldStoreOnlyTheTokenHash_andNeverExposeTheTokenInTheResponse() {
        InviteEmployeeResponse response = inviteEmployeeService.invite(veterinaryId, validRequest);

        verify(invitationRepositoryPort).save(invitationCaptor.capture());
        String storedHash = invitationCaptor.getValue().getTokenHash();
        assertThat(storedHash).hasSize(64).matches("[0-9a-f]{64}");

        // El token en claro solo llega al notificador; la respuesta HTTP no lo contiene en ningún campo.
        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(invitationNotifier).publishInvited(any(EmployeeInvitation.class), tokenCaptor.capture(),
                eq(activeClinic));
        String clearToken = tokenCaptor.getValue();
        assertThat(InvitationTokens.hash(clearToken)).isEqualTo(storedHash);
        assertThat(response.getInvitation().toString()).doesNotContain(clearToken);
    }

    @Test
    void invite_shouldNormalizeEmailToLowerCaseAndTrim() {
        validRequest.setEmail("  NUEVO@Clinica.COM  ");

        inviteEmployeeService.invite(veterinaryId, validRequest);

        verify(invitationRepositoryPort).save(invitationCaptor.capture());
        assertThat(invitationCaptor.getValue().getEmail()).isEqualTo("nuevo@clinica.com");
    }

    @Test
    void invite_shouldThrowInvalidInvitationEmailException_whenEmailIsMissingOrMalformed() {
        validRequest.setEmail(null);
        assertThatThrownBy(() -> inviteEmployeeService.invite(veterinaryId, validRequest))
                .isInstanceOf(InvalidInvitationEmailException.class);

        validRequest.setEmail("no-es-un-correo");
        assertThatThrownBy(() -> inviteEmployeeService.invite(veterinaryId, validRequest))
                .isInstanceOf(InvalidInvitationEmailException.class);

        verify(invitationRepositoryPort, never()).save(any());
    }

    @Test
    void invite_shouldThrowInvalidEmployeeRoleException_whenRoleIsOwnerOrNull() {
        validRequest.setRole(EmployeeRole.OWNER);
        assertThatThrownBy(() -> inviteEmployeeService.invite(veterinaryId, validRequest))
                .isInstanceOf(InvalidEmployeeRoleException.class);

        validRequest.setRole(null);
        assertThatThrownBy(() -> inviteEmployeeService.invite(veterinaryId, validRequest))
                .isInstanceOf(InvalidEmployeeRoleException.class);

        verify(invitationRepositoryPort, never()).save(any());
    }

    @Test
    void invite_shouldAllowOwnerToInviteAdmin() {
        validRequest.setRole(EmployeeRole.ADMIN);

        inviteEmployeeService.invite(veterinaryId, validRequest);

        verify(invitationRepositoryPort).save(invitationCaptor.capture());
        assertThat(invitationCaptor.getValue().getRole()).isEqualTo(EmployeeRole.ADMIN);
    }

    @Test
    void invite_shouldThrowOnlyOwnerCanManageAdminsException_whenActingAdminInvitesAdmin() {
        when(employeeRepositoryPort.findByVeterinaryIdAndUserId(veterinaryId, callerUserId))
                .thenReturn(Optional.of(owner.toBuilder().role(EmployeeRole.ADMIN).build()));
        validRequest.setRole(EmployeeRole.ADMIN);

        assertThatThrownBy(() -> inviteEmployeeService.invite(veterinaryId, validRequest))
                .isInstanceOf(OnlyOwnerCanManageAdminsException.class);

        verify(invitationRepositoryPort, never()).save(any());
    }

    @Test
    void invite_shouldAllowActingAdminToInviteNonAdminRoles() {
        when(employeeRepositoryPort.findByVeterinaryIdAndUserId(veterinaryId, callerUserId))
                .thenReturn(Optional.of(owner.toBuilder().role(EmployeeRole.ADMIN).build()));
        validRequest.setRole(EmployeeRole.RECEPTIONIST);

        inviteEmployeeService.invite(veterinaryId, validRequest);

        verify(invitationRepositoryPort).save(any(EmployeeInvitation.class));
    }

    @Test
    void invite_shouldThrowClinicNotActiveException_whenClinicIsNotApprovedYet() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(
                activeClinic.toBuilder().status(VeterinaryStatus.PENDING_DOCUMENTS).active(false).build()));

        assertThatThrownBy(() -> inviteEmployeeService.invite(veterinaryId, validRequest))
                .isInstanceOf(ClinicNotActiveException.class);

        verify(invitationRepositoryPort, never()).save(any());
    }

    @Test
    void invite_shouldThrowVeterinaryNotFoundException_whenClinicDoesNotExist() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inviteEmployeeService.invite(veterinaryId, validRequest))
                .isInstanceOf(VeterinaryNotFoundException.class);
    }

    @Test
    void invite_shouldThrowEmployeeAlreadyExistsException_whenEmailBelongsToAnActiveEmployee() {
        when(userServicePort.getUsers(anyList())).thenReturn(Map.of(
                callerUserId, new UserServicePort.UserInfo(callerUserId, "Nuevo@clinica.com", true, "Dueño")));

        assertThatThrownBy(() -> inviteEmployeeService.invite(veterinaryId, validRequest))
                .isInstanceOf(EmployeeAlreadyExistsException.class);

        verify(invitationRepositoryPort, never()).save(any());
    }

    @Test
    void invite_shouldIgnoreInactiveEmployeesWhenCheckingForDuplicates() {
        VeterinaryEmployee removed = VeterinaryEmployee.builder()
                .id(UUID.randomUUID())
                .veterinaryId(veterinaryId)
                .userId(UUID.randomUUID())
                .role(EmployeeRole.ASSISTANT)
                .active(false)
                .build();
        when(employeeRepositoryPort.findByVeterinaryId(veterinaryId)).thenReturn(List.of(owner, removed));

        inviteEmployeeService.invite(veterinaryId, validRequest);

        ArgumentCaptor<List<UUID>> idsCaptor = ArgumentCaptor.forClass(List.class);
        verify(userServicePort).getUsers(idsCaptor.capture());
        assertThat(idsCaptor.getValue()).containsExactly(callerUserId);
    }

    @Test
    void invite_shouldThrowInvitationAlreadyPendingException_whenAPendingInvitationIsStillValid() {
        EmployeeInvitation pending = EmployeeInvitation.builder()
                .id(UUID.randomUUID())
                .veterinaryId(veterinaryId)
                .email("nuevo@clinica.com")
                .role(EmployeeRole.ASSISTANT)
                .status(InvitationStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusDays(3))
                .build();
        when(invitationRepositoryPort.findByVeterinaryIdAndEmailAndStatus(
                veterinaryId, "nuevo@clinica.com", InvitationStatus.PENDING)).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> inviteEmployeeService.invite(veterinaryId, validRequest))
                .isInstanceOf(InvitationAlreadyPendingException.class);

        verify(invitationRepositoryPort, never()).save(any());
    }

    @Test
    void invite_shouldRevokeTheExpiredPendingInvitation_beforeCreatingTheNewOne() {
        EmployeeInvitation expired = EmployeeInvitation.builder()
                .id(UUID.randomUUID())
                .veterinaryId(veterinaryId)
                .email("nuevo@clinica.com")
                .role(EmployeeRole.ASSISTANT)
                .status(InvitationStatus.PENDING)
                .expiresAt(LocalDateTime.now().minusDays(1))
                .build();
        when(invitationRepositoryPort.findByVeterinaryIdAndEmailAndStatus(
                veterinaryId, "nuevo@clinica.com", InvitationStatus.PENDING)).thenReturn(Optional.of(expired));

        inviteEmployeeService.invite(veterinaryId, validRequest);

        verify(invitationRepositoryPort, org.mockito.Mockito.times(2)).save(invitationCaptor.capture());
        List<EmployeeInvitation> saved = invitationCaptor.getAllValues();
        // Primero se libera el índice único parcial revocando la vencida, después se crea la nueva.
        assertThat(saved.get(0).getId()).isEqualTo(expired.getId());
        assertThat(saved.get(0).getStatus()).isEqualTo(InvitationStatus.REVOKED);
        assertThat(saved.get(0).getDecidedAt()).isNotNull();
        assertThat(saved.get(1).getId()).isNotEqualTo(expired.getId());
        assertThat(saved.get(1).getStatus()).isEqualTo(InvitationStatus.PENDING);
    }

    @Test
    void invite_shouldCountActiveEmployeesPlusValidPendingInvitationsAsSeatsUsed() {
        VeterinaryEmployee assistant = owner.toBuilder()
                .id(UUID.randomUUID()).userId(UUID.randomUUID()).role(EmployeeRole.ASSISTANT).build();
        when(employeeRepositoryPort.findByVeterinaryId(veterinaryId)).thenReturn(List.of(owner, assistant));
        when(invitationRepositoryPort.findByVeterinaryIdAndStatus(veterinaryId, InvitationStatus.PENDING))
                .thenReturn(List.of(
                        EmployeeInvitation.builder().expiresAt(LocalDateTime.now().plusDays(2)).build(),
                        EmployeeInvitation.builder().expiresAt(LocalDateTime.now().minusDays(2)).build()));

        InviteEmployeeResponse response = inviteEmployeeService.invite(veterinaryId, validRequest);

        // 2 empleados activos + 1 invitación pendiente vigente (la vencida no cuenta).
        assertThat(response.getSeatsUsed()).isEqualTo(3);
    }

    @Test
    void invite_shouldNotPublishAnyEvent_whenAuthorizationFails() {
        org.mockito.Mockito.doThrow(new InsufficientPermissionException(Permission.STAFF_MANAGE))
                .when(authorizationService).require(veterinaryId, Permission.STAFF_MANAGE);

        assertThatThrownBy(() -> inviteEmployeeService.invite(veterinaryId, validRequest))
                .isInstanceOf(InsufficientPermissionException.class);

        verify(invitationNotifier, never()).publishInvited(any(), any(), any());
        verify(invitationRepositoryPort, never()).save(any());
    }
}

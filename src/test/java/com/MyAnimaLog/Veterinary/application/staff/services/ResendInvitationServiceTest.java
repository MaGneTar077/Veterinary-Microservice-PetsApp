package com.MyAnimaLog.Veterinary.application.staff.services;

import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.dto.AuthenticatedUser;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.AuthenticatedUserPort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.application.staff.dto.ResendInvitationResponse;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.EmployeeInvitationRepositoryPort;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.security.Permission;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.domain.staff.enums.InvitationStatus;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.InvitationNotFoundException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.InvitationNotPendingException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.OnlyOwnerCanManageAdminsException;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ResendInvitationServiceTest {

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
    private InvitationNotifier invitationNotifier;

    @InjectMocks
    private ResendInvitationService resendInvitationService;

    private UUID veterinaryId;
    private UUID invitationId;
    private UUID callerUserId;
    private Veterinary activeClinic;
    private VeterinaryEmployee owner;
    private EmployeeInvitation pendingInvitation;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();
        invitationId = UUID.randomUUID();
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

        pendingInvitation = EmployeeInvitation.builder()
                .id(invitationId)
                .veterinaryId(veterinaryId)
                .email("nuevo@clinica.com")
                .role(EmployeeRole.VETERINARIAN)
                .tokenHash(InvitationTokens.hash("token-viejo"))
                .status(InvitationStatus.PENDING)
                .invitedBy(callerUserId)
                .expiresAt(LocalDateTime.now().plusDays(2))
                .createdAt(LocalDateTime.now().minusDays(5))
                .build();

        when(invitationRepositoryPort.findById(invitationId)).thenReturn(Optional.of(pendingInvitation));
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(activeClinic));
        when(authenticatedUserPort.current()).thenReturn(
                new AuthenticatedUser(callerUserId, "owner@clinica.com", true, false, Optional.empty()));
        when(employeeRepositoryPort.findByVeterinaryIdAndUserId(veterinaryId, callerUserId))
                .thenReturn(Optional.of(owner));
        when(invitationRepositoryPort.save(any(EmployeeInvitation.class)))
                .thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void resend_shouldRequireStaffManage() {
        resendInvitationService.resend(veterinaryId, invitationId);

        verify(authorizationService).require(veterinaryId, Permission.STAFF_MANAGE);
    }

    @Test
    void resend_shouldReplaceTheTokenHashAndExtendTheExpiry() {
        String oldHash = pendingInvitation.getTokenHash();
        LocalDateTime before = LocalDateTime.now();

        ResendInvitationResponse response = resendInvitationService.resend(veterinaryId, invitationId);

        ArgumentCaptor<EmployeeInvitation> captor = ArgumentCaptor.forClass(EmployeeInvitation.class);
        verify(invitationRepositoryPort).save(captor.capture());
        EmployeeInvitation resent = captor.getValue();
        assertThat(resent.getTokenHash()).isNotEqualTo(oldHash).hasSize(64);
        assertThat(resent.getExpiresAt()).isAfter(before.plusDays(6)).isBefore(before.plusDays(8));
        assertThat(resent.getStatus()).isEqualTo(InvitationStatus.PENDING);
        assertThat(response.getInvitation().isExpired()).isFalse();
    }

    @Test
    void resend_shouldPublishTheEventWithTheNewToken() {
        resendInvitationService.resend(veterinaryId, invitationId);

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<EmployeeInvitation> invitationCaptor = ArgumentCaptor.forClass(EmployeeInvitation.class);
        verify(invitationNotifier).publishInvited(invitationCaptor.capture(), tokenCaptor.capture(),
                eq(activeClinic));
        assertThat(InvitationTokens.hash(tokenCaptor.getValue()))
                .isEqualTo(invitationCaptor.getValue().getTokenHash());
    }

    @Test
    void resend_shouldWorkOnAnExpiredInvitation() {
        when(invitationRepositoryPort.findById(invitationId)).thenReturn(Optional.of(
                pendingInvitation.toBuilder().expiresAt(LocalDateTime.now().minusDays(3)).build()));

        ResendInvitationResponse response = resendInvitationService.resend(veterinaryId, invitationId);

        assertThat(response.getInvitation().isExpired()).isFalse();
        verify(invitationNotifier).publishInvited(any(), any(), any());
    }

    @Test
    void resend_shouldThrowInvitationNotPendingException_whenAlreadyAcceptedDeclinedOrRevoked() {
        for (InvitationStatus status : new InvitationStatus[]{
                InvitationStatus.ACCEPTED, InvitationStatus.DECLINED, InvitationStatus.REVOKED}) {
            when(invitationRepositoryPort.findById(invitationId)).thenReturn(Optional.of(
                    pendingInvitation.toBuilder().status(status).build()));

            assertThatThrownBy(() -> resendInvitationService.resend(veterinaryId, invitationId))
                    .isInstanceOf(InvitationNotPendingException.class);
        }
        verify(invitationRepositoryPort, never()).save(any());
    }

    @Test
    void resend_shouldThrowInvitationNotFoundException_whenInvitationBelongsToAnotherClinic() {
        when(invitationRepositoryPort.findById(invitationId)).thenReturn(Optional.of(
                pendingInvitation.toBuilder().veterinaryId(UUID.randomUUID()).build()));

        assertThatThrownBy(() -> resendInvitationService.resend(veterinaryId, invitationId))
                .isInstanceOf(InvitationNotFoundException.class);
    }

    @Test
    void resend_shouldThrowInvitationNotFoundException_whenInvitationDoesNotExist() {
        when(invitationRepositoryPort.findById(invitationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resendInvitationService.resend(veterinaryId, invitationId))
                .isInstanceOf(InvitationNotFoundException.class);
    }

    @Test
    void resend_shouldThrowOnlyOwnerCanManageAdminsException_whenActingAdminResendsAnAdminInvitation() {
        when(invitationRepositoryPort.findById(invitationId)).thenReturn(Optional.of(
                pendingInvitation.toBuilder().role(EmployeeRole.ADMIN).build()));
        when(employeeRepositoryPort.findByVeterinaryIdAndUserId(veterinaryId, callerUserId))
                .thenReturn(Optional.of(owner.toBuilder().role(EmployeeRole.ADMIN).build()));

        assertThatThrownBy(() -> resendInvitationService.resend(veterinaryId, invitationId))
                .isInstanceOf(OnlyOwnerCanManageAdminsException.class);

        verify(invitationRepositoryPort, never()).save(any());
        verify(invitationNotifier, never()).publishInvited(any(), any(), any());
    }
}

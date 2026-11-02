package com.MyAnimaLog.Veterinary.application.staff.services;

import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.application.staff.dto.RevokeInvitationResponse;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.EmployeeInvitationRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.security.Permission;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.domain.staff.enums.InvitationStatus;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.InvitationNotFoundException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.InvitationNotPendingException;
import com.MyAnimaLog.Veterinary.domain.staff.model.EmployeeInvitation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RevokeInvitationServiceTest {

    @Mock
    private EmployeeInvitationRepositoryPort invitationRepositoryPort;

    @Mock
    private VeterinaryAuthorizationService authorizationService;

    @InjectMocks
    private RevokeInvitationService revokeInvitationService;

    private UUID veterinaryId;
    private UUID invitationId;
    private EmployeeInvitation pendingInvitation;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();
        invitationId = UUID.randomUUID();
        pendingInvitation = EmployeeInvitation.builder()
                .id(invitationId)
                .veterinaryId(veterinaryId)
                .email("nuevo@clinica.com")
                .role(EmployeeRole.ASSISTANT)
                .status(InvitationStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusDays(4))
                .build();
    }

    @Test
    void revoke_shouldMarkTheInvitationAsRevokedWithADecisionDate() {
        when(invitationRepositoryPort.findById(invitationId)).thenReturn(Optional.of(pendingInvitation));
        when(invitationRepositoryPort.save(any(EmployeeInvitation.class)))
                .thenAnswer(call -> call.getArgument(0));

        RevokeInvitationResponse response = revokeInvitationService.revoke(veterinaryId, invitationId);

        verify(authorizationService).require(veterinaryId, Permission.STAFF_MANAGE);
        assertThat(response.getInvitation().getStatus()).isEqualTo(InvitationStatus.REVOKED);
        assertThat(response.getInvitation().getDecidedAt()).isNotNull();
    }

    @Test
    void revoke_shouldWorkOnAnExpiredPendingInvitation() {
        when(invitationRepositoryPort.findById(invitationId)).thenReturn(Optional.of(
                pendingInvitation.toBuilder().expiresAt(LocalDateTime.now().minusDays(1)).build()));
        when(invitationRepositoryPort.save(any(EmployeeInvitation.class)))
                .thenAnswer(call -> call.getArgument(0));

        RevokeInvitationResponse response = revokeInvitationService.revoke(veterinaryId, invitationId);

        assertThat(response.getInvitation().getStatus()).isEqualTo(InvitationStatus.REVOKED);
    }

    @Test
    void revoke_shouldAllowAnAdminToRevokeAnAdminInvitation() {
        when(invitationRepositoryPort.findById(invitationId)).thenReturn(Optional.of(
                pendingInvitation.toBuilder().role(EmployeeRole.ADMIN).build()));
        when(invitationRepositoryPort.save(any(EmployeeInvitation.class)))
                .thenAnswer(call -> call.getArgument(0));

        RevokeInvitationResponse response = revokeInvitationService.revoke(veterinaryId, invitationId);

        assertThat(response.getInvitation().getStatus()).isEqualTo(InvitationStatus.REVOKED);
    }

    @Test
    void revoke_shouldThrowInvitationNotPendingException_whenAlreadyDecided() {
        when(invitationRepositoryPort.findById(invitationId)).thenReturn(Optional.of(
                pendingInvitation.toBuilder().status(InvitationStatus.ACCEPTED).build()));

        assertThatThrownBy(() -> revokeInvitationService.revoke(veterinaryId, invitationId))
                .isInstanceOf(InvitationNotPendingException.class);

        verify(invitationRepositoryPort, never()).save(any());
    }

    @Test
    void revoke_shouldThrowInvitationNotFoundException_whenInvitationBelongsToAnotherClinic() {
        when(invitationRepositoryPort.findById(invitationId)).thenReturn(Optional.of(
                pendingInvitation.toBuilder().veterinaryId(UUID.randomUUID()).build()));

        assertThatThrownBy(() -> revokeInvitationService.revoke(veterinaryId, invitationId))
                .isInstanceOf(InvitationNotFoundException.class);
    }
}

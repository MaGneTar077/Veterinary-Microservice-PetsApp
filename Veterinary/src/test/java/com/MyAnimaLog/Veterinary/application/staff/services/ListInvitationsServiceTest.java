package com.MyAnimaLog.Veterinary.application.staff.services;

import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.application.staff.dto.ListInvitationsResponse;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.EmployeeInvitationRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.security.Permission;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.InsufficientPermissionException;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.domain.staff.enums.InvitationStatus;
import com.MyAnimaLog.Veterinary.domain.staff.model.EmployeeInvitation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListInvitationsServiceTest {

    @Mock
    private EmployeeInvitationRepositoryPort invitationRepositoryPort;

    @Mock
    private VeterinaryAuthorizationService authorizationService;

    @InjectMocks
    private ListInvitationsService listInvitationsService;

    private UUID veterinaryId;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();
    }

    @Test
    void list_shouldReturnInvitationsWithoutAnyTokenData() {
        EmployeeInvitation invitation = EmployeeInvitation.builder()
                .id(UUID.randomUUID())
                .veterinaryId(veterinaryId)
                .email("nuevo@clinica.com")
                .role(EmployeeRole.ASSISTANT)
                .status(InvitationStatus.PENDING)
                .tokenHash("no-debe-salir-en-la-respuesta")
                .invitedBy(UUID.randomUUID())
                .expiresAt(LocalDateTime.now().plusDays(5))
                .createdAt(LocalDateTime.now())
                .build();
        when(invitationRepositoryPort.findByVeterinaryIdAndStatus(veterinaryId, InvitationStatus.PENDING))
                .thenReturn(List.of(invitation));

        ListInvitationsResponse response = listInvitationsService.list(veterinaryId, InvitationStatus.PENDING);

        assertThat(response.getInvitations()).hasSize(1);
        assertThat(response.getInvitations().get(0).getEmail()).isEqualTo("nuevo@clinica.com");
        assertThat(response.getInvitations().get(0).isExpired()).isFalse();
        assertThat(response.getInvitations().get(0).toString()).doesNotContain("no-debe-salir-en-la-respuesta");
    }

    @Test
    void list_shouldFlagExpiredPendingInvitations() {
        EmployeeInvitation expired = EmployeeInvitation.builder()
                .id(UUID.randomUUID())
                .veterinaryId(veterinaryId)
                .email("vencida@clinica.com")
                .role(EmployeeRole.ASSISTANT)
                .status(InvitationStatus.PENDING)
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .build();
        when(invitationRepositoryPort.findByVeterinaryIdAndStatus(veterinaryId, InvitationStatus.PENDING))
                .thenReturn(List.of(expired));

        ListInvitationsResponse response = listInvitationsService.list(veterinaryId, InvitationStatus.PENDING);

        assertThat(response.getInvitations().get(0).getStatus()).isEqualTo(InvitationStatus.PENDING);
        assertThat(response.getInvitations().get(0).isExpired()).isTrue();
    }

    @Test
    void list_shouldRequireStaffManage() {
        when(invitationRepositoryPort.findByVeterinaryIdAndStatus(veterinaryId, InvitationStatus.REVOKED))
                .thenReturn(List.of());

        listInvitationsService.list(veterinaryId, InvitationStatus.REVOKED);

        verify(authorizationService).require(veterinaryId, Permission.STAFF_MANAGE);
    }

    @Test
    void list_shouldThrowInsufficientPermissionException_whenCallerLacksStaffManage() {
        doThrow(new InsufficientPermissionException(Permission.STAFF_MANAGE))
                .when(authorizationService).require(veterinaryId, Permission.STAFF_MANAGE);

        assertThatThrownBy(() -> listInvitationsService.list(veterinaryId, InvitationStatus.PENDING))
                .isInstanceOf(InsufficientPermissionException.class);
    }
}

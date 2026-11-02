package com.MyAnimaLog.Veterinary.infrastructure.staff.controllers;

import com.MyAnimaLog.Veterinary.application.staff.dto.InvitationSummary;
import com.MyAnimaLog.Veterinary.application.staff.dto.RevokeInvitationResponse;
import com.MyAnimaLog.Veterinary.application.staff.ports.in.RevokeInvitationUseCase;
import com.MyAnimaLog.Veterinary.domain.security.Permission;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.InsufficientPermissionException;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.domain.staff.enums.InvitationStatus;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.InvitationNotFoundException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.InvitationNotPendingException;
import com.MyAnimaLog.Veterinary.infrastructure.config.GlobalExceptionHandler;
import com.MyAnimaLog.Veterinary.infrastructure.security.ImportSecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RevokeInvitationController.class)
@Import(GlobalExceptionHandler.class)
@ImportSecurityConfig
class RevokeInvitationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RevokeInvitationUseCase revokeInvitationUseCase;

    private UUID veterinaryId;
    private UUID invitationId;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();
        invitationId = UUID.randomUUID();
    }

    private ResultActions perform() throws Exception {
        return mockMvc.perform(delete("/api/veterinary/{veterinaryId}/invitations/{invitationId}",
                veterinaryId, invitationId)
                .with(csrf())
                .with(jwt()));
    }

    @Test
    void revoke_shouldReturn200WithTheRevokedInvitation() throws Exception {
        when(revokeInvitationUseCase.revoke(any(UUID.class), any(UUID.class)))
                .thenReturn(RevokeInvitationResponse.builder()
                        .invitation(InvitationSummary.builder()
                                .id(invitationId)
                                .veterinaryId(veterinaryId)
                                .email("nuevo@clinica.com")
                                .role(EmployeeRole.ASSISTANT)
                                .status(InvitationStatus.REVOKED)
                                .decidedAt(LocalDateTime.now())
                                .build())
                        .build());

        perform()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invitation.status").value("REVOKED"))
                .andExpect(jsonPath("$.invitation.decidedAt").exists());
    }

    @Test
    void revoke_shouldReturn404_whenInvitationDoesNotBelongToTheClinic() throws Exception {
        when(revokeInvitationUseCase.revoke(any(UUID.class), any(UUID.class)))
                .thenThrow(new InvitationNotFoundException());

        perform()
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("INVITATION_NOT_FOUND"));
    }

    @Test
    void revoke_shouldReturn409_whenInvitationWasAlreadyDecided() throws Exception {
        when(revokeInvitationUseCase.revoke(any(UUID.class), any(UUID.class)))
                .thenThrow(new InvitationNotPendingException());

        perform()
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVITATION_NOT_PENDING"));
    }

    @Test
    void revoke_shouldReturn403_whenCallerLacksStaffManage() throws Exception {
        when(revokeInvitationUseCase.revoke(any(UUID.class), any(UUID.class)))
                .thenThrow(new InsufficientPermissionException(Permission.STAFF_MANAGE));

        perform().andExpect(status().isForbidden());
    }

    @Test
    void revoke_shouldReturn401_whenNoTokenIsPresent() throws Exception {
        mockMvc.perform(delete("/api/veterinary/{veterinaryId}/invitations/{invitationId}",
                        veterinaryId, invitationId).with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}

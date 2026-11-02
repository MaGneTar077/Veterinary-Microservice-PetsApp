package com.MyAnimaLog.Veterinary.infrastructure.staff.controllers;

import com.MyAnimaLog.Veterinary.application.staff.dto.InvitationSummary;
import com.MyAnimaLog.Veterinary.application.staff.dto.ResendInvitationResponse;
import com.MyAnimaLog.Veterinary.application.staff.ports.in.ResendInvitationUseCase;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.domain.staff.enums.InvitationStatus;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.InvitationNotFoundException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.InvitationNotPendingException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.OnlyOwnerCanManageAdminsException;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ResendInvitationController.class)
@Import(GlobalExceptionHandler.class)
@ImportSecurityConfig
class ResendInvitationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ResendInvitationUseCase resendInvitationUseCase;

    private UUID veterinaryId;
    private UUID invitationId;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();
        invitationId = UUID.randomUUID();
    }

    private ResultActions perform() throws Exception {
        return mockMvc.perform(post("/api/veterinary/{veterinaryId}/invitations/{invitationId}/resend",
                veterinaryId, invitationId)
                .with(csrf())
                .with(jwt()));
    }

    @Test
    void resend_shouldReturn200WithTheRefreshedInvitation() throws Exception {
        when(resendInvitationUseCase.resend(any(UUID.class), any(UUID.class)))
                .thenReturn(ResendInvitationResponse.builder()
                        .invitation(InvitationSummary.builder()
                                .id(invitationId)
                                .veterinaryId(veterinaryId)
                                .email("nuevo@clinica.com")
                                .role(EmployeeRole.ASSISTANT)
                                .status(InvitationStatus.PENDING)
                                .expired(false)
                                .expiresAt(LocalDateTime.now().plusDays(7))
                                .build())
                        .build());

        perform()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invitation.id").value(invitationId.toString()))
                .andExpect(jsonPath("$.invitation.expired").value(false))
                .andExpect(jsonPath("$.invitation.token").doesNotExist());
    }

    @Test
    void resend_shouldReturn404_whenInvitationDoesNotBelongToTheClinic() throws Exception {
        when(resendInvitationUseCase.resend(any(UUID.class), any(UUID.class)))
                .thenThrow(new InvitationNotFoundException());

        perform()
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("INVITATION_NOT_FOUND"));
    }

    @Test
    void resend_shouldReturn409_whenInvitationIsNoLongerPending() throws Exception {
        when(resendInvitationUseCase.resend(any(UUID.class), any(UUID.class)))
                .thenThrow(new InvitationNotPendingException());

        perform()
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVITATION_NOT_PENDING"));
    }

    @Test
    void resend_shouldReturn403_whenActingAdminResendsAnAdminInvitation() throws Exception {
        when(resendInvitationUseCase.resend(any(UUID.class), any(UUID.class)))
                .thenThrow(new OnlyOwnerCanManageAdminsException());

        perform()
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ONLY_OWNER_CAN_MANAGE_ADMINS"));
    }

    @Test
    void resend_shouldReturn401_whenNoTokenIsPresent() throws Exception {
        mockMvc.perform(post("/api/veterinary/{veterinaryId}/invitations/{invitationId}/resend",
                        veterinaryId, invitationId).with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}

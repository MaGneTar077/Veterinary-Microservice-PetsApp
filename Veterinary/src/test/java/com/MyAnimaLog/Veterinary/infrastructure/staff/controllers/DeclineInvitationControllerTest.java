package com.MyAnimaLog.Veterinary.infrastructure.staff.controllers;

import com.MyAnimaLog.Veterinary.application.staff.dto.DeclineInvitationRequest;
import com.MyAnimaLog.Veterinary.application.staff.dto.DeclineInvitationResponse;
import com.MyAnimaLog.Veterinary.application.staff.ports.in.DeclineInvitationUseCase;
import com.MyAnimaLog.Veterinary.domain.staff.enums.InvitationStatus;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.InvitationEmailMismatchException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.InvitationExpiredException;
import com.MyAnimaLog.Veterinary.infrastructure.config.GlobalExceptionHandler;
import com.MyAnimaLog.Veterinary.infrastructure.security.ImportSecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
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

@WebMvcTest(controllers = DeclineInvitationController.class)
@Import(GlobalExceptionHandler.class)
@ImportSecurityConfig
class DeclineInvitationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DeclineInvitationUseCase declineInvitationUseCase;

    private ResultActions perform() throws Exception {
        return mockMvc.perform(post("/api/veterinary/invitations/decline")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        DeclineInvitationRequest.builder().token("token-de-prueba").build()))
                .with(csrf())
                .with(jwt()));
    }

    @Test
    void decline_shouldReturn200WithTheDeclinedInvitation() throws Exception {
        UUID invitationId = UUID.randomUUID();
        when(declineInvitationUseCase.decline(any(DeclineInvitationRequest.class)))
                .thenReturn(DeclineInvitationResponse.builder()
                        .invitationId(invitationId)
                        .veterinaryId(UUID.randomUUID())
                        .status(InvitationStatus.DECLINED)
                        .decidedAt(LocalDateTime.now())
                        .build());

        perform()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invitationId").value(invitationId.toString()))
                .andExpect(jsonPath("$.status").value("DECLINED"));
    }

    @Test
    void decline_shouldReturn403_whenTheTokenEmailIsNotTheInvitedOne() throws Exception {
        when(declineInvitationUseCase.decline(any(DeclineInvitationRequest.class)))
                .thenThrow(new InvitationEmailMismatchException());

        perform()
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("INVITATION_EMAIL_MISMATCH"));
    }

    @Test
    void decline_shouldReturn410_whenTheInvitationExpired() throws Exception {
        when(declineInvitationUseCase.decline(any(DeclineInvitationRequest.class)))
                .thenThrow(new InvitationExpiredException());

        perform()
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("INVITATION_EXPIRED"));
    }

    @Test
    void decline_shouldReturn401_whenNoTokenIsPresent() throws Exception {
        mockMvc.perform(post("/api/veterinary/invitations/decline")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                DeclineInvitationRequest.builder().token("token-de-prueba").build()))
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}

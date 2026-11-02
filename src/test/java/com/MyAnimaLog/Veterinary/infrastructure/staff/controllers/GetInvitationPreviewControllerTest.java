package com.MyAnimaLog.Veterinary.infrastructure.staff.controllers;

import com.MyAnimaLog.Veterinary.application.staff.dto.GetInvitationPreviewRequest;
import com.MyAnimaLog.Veterinary.application.staff.dto.GetInvitationPreviewResponse;
import com.MyAnimaLog.Veterinary.application.staff.ports.in.GetInvitationPreviewUseCase;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.domain.staff.enums.InvitationStatus;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.InvitationNotFoundException;
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

@WebMvcTest(controllers = GetInvitationPreviewController.class)
@Import(GlobalExceptionHandler.class)
@ImportSecurityConfig
class GetInvitationPreviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private GetInvitationPreviewUseCase getInvitationPreviewUseCase;

    private ResultActions perform() throws Exception {
        return mockMvc.perform(post("/api/veterinary/invitations/preview")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        GetInvitationPreviewRequest.builder().token("token-de-prueba").build()))
                .with(csrf())
                .with(jwt()));
    }

    @Test
    void preview_shouldReturn200WithTheClinicAndRole() throws Exception {
        when(getInvitationPreviewUseCase.preview(any(GetInvitationPreviewRequest.class)))
                .thenReturn(GetInvitationPreviewResponse.builder()
                        .invitationId(UUID.randomUUID())
                        .veterinaryId(UUID.randomUUID())
                        .veterinaryName("Clínica El Bosque")
                        .veterinaryCity("Bogotá")
                        .logoUrl("https://cdn/logo.png")
                        .role(EmployeeRole.RECEPTIONIST)
                        .invitedByName("Ana Dueña")
                        .email("invitado@clinica.com")
                        .emailMatchesCaller(true)
                        .status(InvitationStatus.PENDING)
                        .expired(false)
                        .usable(true)
                        .expiresAt(LocalDateTime.now().plusDays(5))
                        .build());

        perform()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.veterinaryName").value("Clínica El Bosque"))
                .andExpect(jsonPath("$.role").value("RECEPTIONIST"))
                .andExpect(jsonPath("$.invitedByName").value("Ana Dueña"))
                .andExpect(jsonPath("$.usable").value(true))
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void preview_shouldReturn200AndNotUsable_whenTheInvitationIsNoLongerValid() throws Exception {
        when(getInvitationPreviewUseCase.preview(any(GetInvitationPreviewRequest.class)))
                .thenReturn(GetInvitationPreviewResponse.builder()
                        .invitationId(UUID.randomUUID())
                        .status(InvitationStatus.PENDING)
                        .expired(true)
                        .usable(false)
                        .build());

        perform()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expired").value(true))
                .andExpect(jsonPath("$.usable").value(false));
    }

    @Test
    void preview_shouldReturn404_whenTheTokenIsUnknown() throws Exception {
        when(getInvitationPreviewUseCase.preview(any(GetInvitationPreviewRequest.class)))
                .thenThrow(new InvitationNotFoundException());

        perform()
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("INVITATION_NOT_FOUND"));
    }

    @Test
    void preview_shouldReturn401_whenNoTokenIsPresent() throws Exception {
        mockMvc.perform(post("/api/veterinary/invitations/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                GetInvitationPreviewRequest.builder().token("token-de-prueba").build()))
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}

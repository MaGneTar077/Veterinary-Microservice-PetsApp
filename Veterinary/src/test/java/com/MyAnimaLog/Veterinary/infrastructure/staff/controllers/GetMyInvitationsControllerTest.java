package com.MyAnimaLog.Veterinary.infrastructure.staff.controllers;

import com.MyAnimaLog.Veterinary.application.staff.dto.GetMyInvitationsResponse;
import com.MyAnimaLog.Veterinary.application.staff.ports.in.GetMyInvitationsUseCase;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.infrastructure.config.GlobalExceptionHandler;
import com.MyAnimaLog.Veterinary.infrastructure.security.ImportSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GetMyInvitationsController.class)
@Import(GlobalExceptionHandler.class)
@ImportSecurityConfig
class GetMyInvitationsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetMyInvitationsUseCase getMyInvitationsUseCase;

    @Test
    void myInvitations_shouldReturn200WithThePendingInvitations() throws Exception {
        UUID invitationId = UUID.randomUUID();
        when(getMyInvitationsUseCase.myInvitations()).thenReturn(GetMyInvitationsResponse.builder()
                .invitations(List.of(GetMyInvitationsResponse.MyInvitation.builder()
                        .invitationId(invitationId)
                        .veterinaryId(UUID.randomUUID())
                        .veterinaryName("Clínica El Bosque")
                        .veterinaryCity("Bogotá")
                        .role(EmployeeRole.VETERINARIAN)
                        .expiresAt(LocalDateTime.now().plusDays(5))
                        .build()))
                .build());

        mockMvc.perform(get("/api/veterinary/me/invitations").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invitations[0].invitationId").value(invitationId.toString()))
                .andExpect(jsonPath("$.invitations[0].veterinaryName").value("Clínica El Bosque"))
                .andExpect(jsonPath("$.invitations[0].role").value("VETERINARIAN"))
                .andExpect(jsonPath("$.invitations[0].token").doesNotExist());
    }

    @Test
    void myInvitations_shouldReturn200WithAnEmptyList_whenThereAreNone() throws Exception {
        when(getMyInvitationsUseCase.myInvitations()).thenReturn(GetMyInvitationsResponse.builder()
                .invitations(List.of())
                .build());

        mockMvc.perform(get("/api/veterinary/me/invitations").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invitations").isEmpty());
    }

    @Test
    void myInvitations_shouldReturn401_whenNoTokenIsPresent() throws Exception {
        mockMvc.perform(get("/api/veterinary/me/invitations"))
                .andExpect(status().isUnauthorized());
    }
}

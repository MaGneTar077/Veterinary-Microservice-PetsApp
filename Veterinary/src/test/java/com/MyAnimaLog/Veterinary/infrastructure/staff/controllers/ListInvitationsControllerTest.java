package com.MyAnimaLog.Veterinary.infrastructure.staff.controllers;

import com.MyAnimaLog.Veterinary.application.staff.dto.InvitationSummary;
import com.MyAnimaLog.Veterinary.application.staff.dto.ListInvitationsResponse;
import com.MyAnimaLog.Veterinary.application.staff.ports.in.ListInvitationsUseCase;
import com.MyAnimaLog.Veterinary.domain.security.Permission;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.InsufficientPermissionException;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.domain.staff.enums.InvitationStatus;
import com.MyAnimaLog.Veterinary.infrastructure.config.GlobalExceptionHandler;
import com.MyAnimaLog.Veterinary.infrastructure.security.ImportSecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ListInvitationsController.class)
@Import(GlobalExceptionHandler.class)
@ImportSecurityConfig
class ListInvitationsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListInvitationsUseCase listInvitationsUseCase;

    private UUID veterinaryId;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();
        when(listInvitationsUseCase.list(any(UUID.class), any(InvitationStatus.class)))
                .thenReturn(ListInvitationsResponse.builder()
                        .invitations(List.of(InvitationSummary.builder()
                                .id(UUID.randomUUID())
                                .veterinaryId(veterinaryId)
                                .email("nuevo@clinica.com")
                                .role(EmployeeRole.ASSISTANT)
                                .status(InvitationStatus.PENDING)
                                .expired(false)
                                .expiresAt(LocalDateTime.now().plusDays(7))
                                .build()))
                        .build());
    }

    @Test
    void list_shouldReturn200AndDefaultToPendingInvitations() throws Exception {
        mockMvc.perform(get("/api/veterinary/{veterinaryId}/invitations", veterinaryId).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invitations[0].email").value("nuevo@clinica.com"))
                .andExpect(jsonPath("$.invitations[0].expired").value(false))
                .andExpect(jsonPath("$.invitations[0].tokenHash").doesNotExist());

        verify(listInvitationsUseCase).list(veterinaryId, InvitationStatus.PENDING);
    }

    @Test
    void list_shouldPassTheRequestedStatusThrough() throws Exception {
        mockMvc.perform(get("/api/veterinary/{veterinaryId}/invitations", veterinaryId)
                        .param("status", "REVOKED")
                        .with(jwt()))
                .andExpect(status().isOk());

        verify(listInvitationsUseCase).list(eq(veterinaryId), eq(InvitationStatus.REVOKED));
    }

    @Test
    void list_shouldReturn403_whenCallerLacksStaffManage() throws Exception {
        when(listInvitationsUseCase.list(any(UUID.class), any(InvitationStatus.class)))
                .thenThrow(new InsufficientPermissionException(Permission.STAFF_MANAGE));

        mockMvc.perform(get("/api/veterinary/{veterinaryId}/invitations", veterinaryId).with(jwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void list_shouldReturn401_whenNoTokenIsPresent() throws Exception {
        mockMvc.perform(get("/api/veterinary/{veterinaryId}/invitations", veterinaryId))
                .andExpect(status().isUnauthorized());
    }
}

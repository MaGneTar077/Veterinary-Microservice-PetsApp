package com.MyAnimaLog.Veterinary.infrastructure.staff.controllers;

import com.MyAnimaLog.Veterinary.application.staff.dto.InvitationSummary;
import com.MyAnimaLog.Veterinary.application.staff.dto.InviteEmployeeRequest;
import com.MyAnimaLog.Veterinary.application.staff.dto.InviteEmployeeResponse;
import com.MyAnimaLog.Veterinary.application.staff.ports.in.InviteEmployeeUseCase;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.ClinicNotActiveException;
import com.MyAnimaLog.Veterinary.domain.security.Permission;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.InsufficientPermissionException;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.domain.staff.enums.InvitationStatus;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.EmployeeAlreadyExistsException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.InvalidInvitationEmailException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.InvitationAlreadyPendingException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.OnlyOwnerCanManageAdminsException;
import com.MyAnimaLog.Veterinary.infrastructure.config.GlobalExceptionHandler;
import com.MyAnimaLog.Veterinary.infrastructure.security.ImportSecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = InviteEmployeeController.class)
@Import(GlobalExceptionHandler.class)
@ImportSecurityConfig
class InviteEmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InviteEmployeeUseCase inviteEmployeeUseCase;

    private UUID veterinaryId;
    private InviteEmployeeRequest validRequest;
    private InviteEmployeeResponse validResponse;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();
        validRequest = InviteEmployeeRequest.builder()
                .email("nuevo@clinica.com")
                .role(EmployeeRole.VETERINARIAN)
                .build();
        validResponse = InviteEmployeeResponse.builder()
                .invitation(InvitationSummary.builder()
                        .id(UUID.randomUUID())
                        .veterinaryId(veterinaryId)
                        .email("nuevo@clinica.com")
                        .role(EmployeeRole.VETERINARIAN)
                        .status(InvitationStatus.PENDING)
                        .expired(false)
                        .expiresAt(LocalDateTime.now().plusDays(7))
                        .build())
                .seatsUsed(3)
                .build();
    }

    private org.springframework.test.web.servlet.ResultActions perform(InviteEmployeeRequest request) throws Exception {
        return mockMvc.perform(post("/api/veterinary/{veterinaryId}/invitations", veterinaryId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(csrf())
                .with(jwt()));
    }

    @Test
    void invite_shouldReturn201_whenRequestIsValid() throws Exception {
        when(inviteEmployeeUseCase.invite(any(UUID.class), any(InviteEmployeeRequest.class)))
                .thenReturn(validResponse);

        perform(validRequest)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.invitation.email").value("nuevo@clinica.com"))
                .andExpect(jsonPath("$.invitation.status").value("PENDING"))
                .andExpect(jsonPath("$.invitation.role").value("VETERINARIAN"))
                .andExpect(jsonPath("$.seatsUsed").value(3))
                .andExpect(jsonPath("$.invitation.token").doesNotExist())
                .andExpect(jsonPath("$.invitation.tokenHash").doesNotExist());
    }

    @Test
    void invite_shouldReturn400_whenEmailIsInvalid() throws Exception {
        when(inviteEmployeeUseCase.invite(any(UUID.class), any(InviteEmployeeRequest.class)))
                .thenThrow(new InvalidInvitationEmailException());

        perform(validRequest)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void invite_shouldReturn403_whenActingAdminInvitesAnAdmin() throws Exception {
        when(inviteEmployeeUseCase.invite(any(UUID.class), any(InviteEmployeeRequest.class)))
                .thenThrow(new OnlyOwnerCanManageAdminsException());

        perform(validRequest)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ONLY_OWNER_CAN_MANAGE_ADMINS"));
    }

    @Test
    void invite_shouldReturn403_whenCallerLacksStaffManage() throws Exception {
        when(inviteEmployeeUseCase.invite(any(UUID.class), any(InviteEmployeeRequest.class)))
                .thenThrow(new InsufficientPermissionException(Permission.STAFF_MANAGE));

        perform(validRequest).andExpect(status().isForbidden());
    }

    @Test
    void invite_shouldReturn409_whenThereIsAlreadyAPendingInvitation() throws Exception {
        when(inviteEmployeeUseCase.invite(any(UUID.class), any(InviteEmployeeRequest.class)))
                .thenThrow(new InvitationAlreadyPendingException());

        perform(validRequest)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVITATION_ALREADY_PENDING"));
    }

    @Test
    void invite_shouldReturn409_whenEmailIsAlreadyAnActiveEmployee() throws Exception {
        when(inviteEmployeeUseCase.invite(any(UUID.class), any(InviteEmployeeRequest.class)))
                .thenThrow(new EmployeeAlreadyExistsException());

        perform(validRequest)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMPLOYEE_ALREADY_EXISTS"));
    }

    @Test
    void invite_shouldReturn409_whenClinicIsNotActive() throws Exception {
        when(inviteEmployeeUseCase.invite(any(UUID.class), any(InviteEmployeeRequest.class)))
                .thenThrow(new ClinicNotActiveException());

        perform(validRequest)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CLINIC_NOT_ACTIVE"));
    }

    @Test
    void invite_shouldReturn401_whenNoTokenIsPresent() throws Exception {
        mockMvc.perform(post("/api/veterinary/{veterinaryId}/invitations", veterinaryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest))
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}

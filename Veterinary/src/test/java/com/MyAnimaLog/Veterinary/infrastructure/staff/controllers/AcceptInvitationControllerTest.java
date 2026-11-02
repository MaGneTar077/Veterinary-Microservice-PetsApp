package com.MyAnimaLog.Veterinary.infrastructure.staff.controllers;

import com.MyAnimaLog.Veterinary.application.staff.dto.AcceptInvitationRequest;
import com.MyAnimaLog.Veterinary.application.staff.dto.AcceptInvitationResponse;
import com.MyAnimaLog.Veterinary.application.staff.ports.in.AcceptInvitationUseCase;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.ClinicNotActiveException;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.EmailNotVerifiedException;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.EmployeeAlreadyExistsException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.InvitationEmailMismatchException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.InvitationExpiredException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.InvitationNotFoundException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.InvitationNotPendingException;
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

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AcceptInvitationController.class)
@Import(GlobalExceptionHandler.class)
@ImportSecurityConfig
class AcceptInvitationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AcceptInvitationUseCase acceptInvitationUseCase;

    private ResultActions perform() throws Exception {
        return mockMvc.perform(post("/api/veterinary/invitations/accept")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        AcceptInvitationRequest.builder().token("token-de-prueba").build()))
                .with(csrf())
                .with(jwt()));
    }

    @Test
    void accept_shouldReturn200WithTheNewEmployee() throws Exception {
        UUID employeeId = UUID.randomUUID();
        when(acceptInvitationUseCase.accept(any(AcceptInvitationRequest.class)))
                .thenReturn(AcceptInvitationResponse.builder()
                        .employeeId(employeeId)
                        .veterinaryId(UUID.randomUUID())
                        .veterinaryName("Clínica El Bosque")
                        .role(EmployeeRole.VETERINARIAN)
                        .licensed(false)
                        .reactivated(false)
                        .build());

        perform()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeId").value(employeeId.toString()))
                .andExpect(jsonPath("$.role").value("VETERINARIAN"))
                .andExpect(jsonPath("$.licensed").value(false))
                .andExpect(jsonPath("$.reactivated").value(false));
    }

    @Test
    void accept_shouldReturn403_whenTheTokenEmailIsNotTheInvitedOne() throws Exception {
        when(acceptInvitationUseCase.accept(any(AcceptInvitationRequest.class)))
                .thenThrow(new InvitationEmailMismatchException());

        perform()
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("INVITATION_EMAIL_MISMATCH"));
    }

    @Test
    void accept_shouldReturn403_whenTheCallerEmailIsNotVerified() throws Exception {
        when(acceptInvitationUseCase.accept(any(AcceptInvitationRequest.class)))
                .thenThrow(new EmailNotVerifiedException());

        perform()
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));
    }

    @Test
    void accept_shouldReturn404_whenTheTokenIsUnknown() throws Exception {
        when(acceptInvitationUseCase.accept(any(AcceptInvitationRequest.class)))
                .thenThrow(new InvitationNotFoundException());

        perform()
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("INVITATION_NOT_FOUND"));
    }

    @Test
    void accept_shouldReturn409_whenTheInvitationWasRevokedOrAlreadyUsed() throws Exception {
        when(acceptInvitationUseCase.accept(any(AcceptInvitationRequest.class)))
                .thenThrow(new InvitationNotPendingException());

        perform()
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVITATION_NOT_PENDING"));
    }

    @Test
    void accept_shouldReturn410_whenTheInvitationExpired() throws Exception {
        when(acceptInvitationUseCase.accept(any(AcceptInvitationRequest.class)))
                .thenThrow(new InvitationExpiredException());

        perform()
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("INVITATION_EXPIRED"));
    }

    @Test
    void accept_shouldReturn409_whenTheUserIsAlreadyAnActiveEmployee() throws Exception {
        when(acceptInvitationUseCase.accept(any(AcceptInvitationRequest.class)))
                .thenThrow(new EmployeeAlreadyExistsException());

        perform()
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMPLOYEE_ALREADY_EXISTS"));
    }

    @Test
    void accept_shouldReturn409_whenTheClinicIsNotActive() throws Exception {
        when(acceptInvitationUseCase.accept(any(AcceptInvitationRequest.class)))
                .thenThrow(new ClinicNotActiveException());

        perform()
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CLINIC_NOT_ACTIVE"));
    }

    @Test
    void accept_shouldReturn401_whenNoTokenIsPresent() throws Exception {
        mockMvc.perform(post("/api/veterinary/invitations/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                AcceptInvitationRequest.builder().token("token-de-prueba").build()))
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}

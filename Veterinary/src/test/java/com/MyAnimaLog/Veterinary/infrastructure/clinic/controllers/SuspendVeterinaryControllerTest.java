package com.MyAnimaLog.Veterinary.infrastructure.clinic.controllers;

import com.MyAnimaLog.Veterinary.application.clinic.dto.SuspendVeterinaryRequest;
import com.MyAnimaLog.Veterinary.application.clinic.dto.SuspendVeterinaryResponse;
import com.MyAnimaLog.Veterinary.application.clinic.ports.in.SuspendVeterinaryUseCase;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.CannotManageOwnClinicException;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.ClinicNotActiveException;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.InvalidSuspensionReasonException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VeterinaryNotFoundException;
import com.MyAnimaLog.Veterinary.infrastructure.config.GlobalExceptionHandler;
import com.MyAnimaLog.Veterinary.infrastructure.security.ImportSecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = SuspendVeterinaryController.class)
@Import(GlobalExceptionHandler.class)
@ImportSecurityConfig
class SuspendVeterinaryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SuspendVeterinaryUseCase suspendVeterinaryUseCase;

    private UUID veterinaryId;
    private SuspendVeterinaryResponse validResponse;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();
        validResponse = SuspendVeterinaryResponse.builder()
                .id(veterinaryId)
                .name("Clínica El Bosque")
                .status(VeterinaryStatus.SUSPENDED)
                .active(false)
                .suspensionReason("Pago atrasado por mas de 60 dias")
                .build();
    }

    private String body(String reason) throws Exception {
        return objectMapper.writeValueAsString(SuspendVeterinaryRequest.builder().reason(reason).build());
    }

    @Test
    void suspend_shouldReturn200_whenCallerIsPlatformAdmin() throws Exception {
        when(suspendVeterinaryUseCase.suspend(any(UUID.class), any())).thenReturn(validResponse);

        mockMvc.perform(patch("/api/veterinary/admin/veterinaries/{veterinaryId}/suspend", veterinaryId)
                        .contentType("application/json")
                        .content(body("Pago atrasado por mas de 60 dias"))
                        .with(csrf())
                        .with(jwt().authorities(new SimpleGrantedAuthority("PLATFORM_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"))
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void suspend_shouldReturn404_whenVeterinaryNotFound() throws Exception {
        when(suspendVeterinaryUseCase.suspend(any(UUID.class), any())).thenThrow(new VeterinaryNotFoundException());

        mockMvc.perform(patch("/api/veterinary/admin/veterinaries/{veterinaryId}/suspend", veterinaryId)
                        .contentType("application/json")
                        .content(body("Pago atrasado por mas de 60 dias"))
                        .with(csrf())
                        .with(jwt().authorities(new SimpleGrantedAuthority("PLATFORM_ADMIN"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void suspend_shouldReturn409_whenNotCurrentlyActive() throws Exception {
        when(suspendVeterinaryUseCase.suspend(any(UUID.class), any()))
                .thenThrow(new ClinicNotActiveException());

        mockMvc.perform(patch("/api/veterinary/admin/veterinaries/{veterinaryId}/suspend", veterinaryId)
                        .contentType("application/json")
                        .content(body("Pago atrasado por mas de 60 dias"))
                        .with(csrf())
                        .with(jwt().authorities(new SimpleGrantedAuthority("PLATFORM_ADMIN"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CLINIC_NOT_ACTIVE"));
    }

    @Test
    void suspend_shouldReturn403_whenAdminIsEmployeeOfTheClinic() throws Exception {
        when(suspendVeterinaryUseCase.suspend(any(UUID.class), any()))
                .thenThrow(new CannotManageOwnClinicException());

        mockMvc.perform(patch("/api/veterinary/admin/veterinaries/{veterinaryId}/suspend", veterinaryId)
                        .contentType("application/json")
                        .content(body("Pago atrasado por mas de 60 dias"))
                        .with(csrf())
                        .with(jwt().authorities(new SimpleGrantedAuthority("PLATFORM_ADMIN"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CANNOT_MANAGE_OWN_CLINIC"));
    }

    @Test
    void suspend_shouldReturn400_whenReasonIsTooShort() throws Exception {
        when(suspendVeterinaryUseCase.suspend(any(UUID.class), any()))
                .thenThrow(new InvalidSuspensionReasonException("reason must be between 10 and 500 characters"));

        mockMvc.perform(patch("/api/veterinary/admin/veterinaries/{veterinaryId}/suspend", veterinaryId)
                        .contentType("application/json")
                        .content(body("short"))
                        .with(csrf())
                        .with(jwt().authorities(new SimpleGrantedAuthority("PLATFORM_ADMIN"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void suspend_shouldReturn403_whenCallerLacksPlatformAdminAuthority() throws Exception {
        mockMvc.perform(patch("/api/veterinary/admin/veterinaries/{veterinaryId}/suspend", veterinaryId)
                        .contentType("application/json")
                        .content(body("Pago atrasado por mas de 60 dias"))
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void suspend_shouldReturn401_whenNoTokenIsPresent() throws Exception {
        mockMvc.perform(patch("/api/veterinary/admin/veterinaries/{veterinaryId}/suspend", veterinaryId)
                        .contentType("application/json")
                        .content(body("Pago atrasado por mas de 60 dias"))
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}

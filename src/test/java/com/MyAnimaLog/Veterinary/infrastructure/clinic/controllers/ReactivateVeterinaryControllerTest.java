package com.MyAnimaLog.Veterinary.infrastructure.clinic.controllers;

import com.MyAnimaLog.Veterinary.application.clinic.dto.ReactivateVeterinaryResponse;
import com.MyAnimaLog.Veterinary.application.clinic.ports.in.ReactivateVeterinaryUseCase;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.CannotManageOwnClinicException;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.ClinicNotSuspendedException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VeterinaryNotFoundException;
import com.MyAnimaLog.Veterinary.infrastructure.config.GlobalExceptionHandler;
import com.MyAnimaLog.Veterinary.infrastructure.security.ImportSecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ReactivateVeterinaryController.class)
@Import(GlobalExceptionHandler.class)
@ImportSecurityConfig
class ReactivateVeterinaryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReactivateVeterinaryUseCase reactivateVeterinaryUseCase;

    private UUID veterinaryId;
    private ReactivateVeterinaryResponse validResponse;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();
        validResponse = ReactivateVeterinaryResponse.builder()
                .id(veterinaryId)
                .name("Clínica El Bosque")
                .status(VeterinaryStatus.ACTIVE)
                .active(true)
                .approvedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void reactivate_shouldReturn200_whenCallerIsPlatformAdmin() throws Exception {
        when(reactivateVeterinaryUseCase.reactivate(any(UUID.class))).thenReturn(validResponse);

        mockMvc.perform(patch("/api/veterinary/admin/veterinaries/{veterinaryId}/reactivate", veterinaryId)
                        .with(csrf())
                        .with(jwt().authorities(new SimpleGrantedAuthority("PLATFORM_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void reactivate_shouldReturn404_whenVeterinaryNotFound() throws Exception {
        when(reactivateVeterinaryUseCase.reactivate(any(UUID.class))).thenThrow(new VeterinaryNotFoundException());

        mockMvc.perform(patch("/api/veterinary/admin/veterinaries/{veterinaryId}/reactivate", veterinaryId)
                        .with(csrf())
                        .with(jwt().authorities(new SimpleGrantedAuthority("PLATFORM_ADMIN"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void reactivate_shouldReturn409_whenStatusDoesNotAllowReactivation() throws Exception {
        when(reactivateVeterinaryUseCase.reactivate(any(UUID.class)))
                .thenThrow(new ClinicNotSuspendedException());

        mockMvc.perform(patch("/api/veterinary/admin/veterinaries/{veterinaryId}/reactivate", veterinaryId)
                        .with(csrf())
                        .with(jwt().authorities(new SimpleGrantedAuthority("PLATFORM_ADMIN"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CLINIC_NOT_SUSPENDED"));
    }

    @Test
    void reactivate_shouldReturn403_whenAdminIsEmployeeOfTheClinic() throws Exception {
        when(reactivateVeterinaryUseCase.reactivate(any(UUID.class)))
                .thenThrow(new CannotManageOwnClinicException());

        mockMvc.perform(patch("/api/veterinary/admin/veterinaries/{veterinaryId}/reactivate", veterinaryId)
                        .with(csrf())
                        .with(jwt().authorities(new SimpleGrantedAuthority("PLATFORM_ADMIN"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CANNOT_MANAGE_OWN_CLINIC"));
    }

    @Test
    void reactivate_shouldReturn403_whenCallerLacksPlatformAdminAuthority() throws Exception {
        mockMvc.perform(patch("/api/veterinary/admin/veterinaries/{veterinaryId}/reactivate", veterinaryId)
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void reactivate_shouldReturn401_whenNoTokenIsPresent() throws Exception {
        mockMvc.perform(patch("/api/veterinary/admin/veterinaries/{veterinaryId}/reactivate", veterinaryId)
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}

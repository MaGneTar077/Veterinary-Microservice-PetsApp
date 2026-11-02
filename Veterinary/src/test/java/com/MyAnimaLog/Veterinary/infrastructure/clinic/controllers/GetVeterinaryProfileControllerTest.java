package com.MyAnimaLog.Veterinary.infrastructure.clinic.controllers;

import com.MyAnimaLog.Veterinary.application.clinic.dto.GetVeterinaryProfileResponse;
import com.MyAnimaLog.Veterinary.application.clinic.ports.in.GetVeterinaryProfileUseCase;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.ClinicContextRequiredException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.TenantMismatchException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VeterinaryNotFoundException;
import com.MyAnimaLog.Veterinary.infrastructure.config.GlobalExceptionHandler;
import com.MyAnimaLog.Veterinary.infrastructure.security.ImportSecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = GetVeterinaryProfileController.class)
@Import(GlobalExceptionHandler.class)
@ImportSecurityConfig
class GetVeterinaryProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetVeterinaryProfileUseCase getVeterinaryProfileUseCase;

    private UUID veterinaryId;
    private GetVeterinaryProfileResponse validResponse;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();
        validResponse = GetVeterinaryProfileResponse.builder()
                .id(veterinaryId)
                .name("Clínica El Bosque")
                .status(VeterinaryStatus.ACTIVE)
                .build();
    }

    @Test
    void getProfile_shouldReturn200_whenCallerIsMember() throws Exception {
        when(getVeterinaryProfileUseCase.getProfile(any(UUID.class))).thenReturn(validResponse);

        mockMvc.perform(get("/api/veterinary/{veterinaryId}", veterinaryId)
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Clínica El Bosque"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void getProfile_shouldReturn404_whenVeterinaryNotFound() throws Exception {
        when(getVeterinaryProfileUseCase.getProfile(any(UUID.class))).thenThrow(new VeterinaryNotFoundException());

        mockMvc.perform(get("/api/veterinary/{veterinaryId}", veterinaryId)
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getProfile_shouldReturn403_whenCallerHasNoClinicToken() throws Exception {
        when(getVeterinaryProfileUseCase.getProfile(any(UUID.class)))
                .thenThrow(new ClinicContextRequiredException());

        mockMvc.perform(get("/api/veterinary/{veterinaryId}", veterinaryId)
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void getProfile_shouldReturn403_whenCallerBelongsToAnotherVeterinary() throws Exception {
        when(getVeterinaryProfileUseCase.getProfile(any(UUID.class)))
                .thenThrow(new TenantMismatchException());

        mockMvc.perform(get("/api/veterinary/{veterinaryId}", veterinaryId)
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void getProfile_shouldReturn401_whenNoTokenIsPresent() throws Exception {
        mockMvc.perform(get("/api/veterinary/{veterinaryId}", veterinaryId))
                .andExpect(status().isUnauthorized());
    }
}

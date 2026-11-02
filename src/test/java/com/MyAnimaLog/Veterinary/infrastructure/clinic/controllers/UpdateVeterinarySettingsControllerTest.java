package com.MyAnimaLog.Veterinary.infrastructure.clinic.controllers;

import com.MyAnimaLog.Veterinary.application.clinic.dto.UpdateVeterinarySettingsRequest;
import com.MyAnimaLog.Veterinary.application.clinic.dto.UpdateVeterinarySettingsResponse;
import com.MyAnimaLog.Veterinary.application.clinic.ports.in.UpdateVeterinarySettingsUseCase;
import com.MyAnimaLog.Veterinary.domain.security.Permission;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.InsufficientPermissionException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VeterinaryNotFoundException;
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

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UpdateVeterinarySettingsController.class)
@Import(GlobalExceptionHandler.class)
@ImportSecurityConfig
class UpdateVeterinarySettingsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UpdateVeterinarySettingsUseCase updateVeterinarySettingsUseCase;

    private UUID veterinaryId;
    private UpdateVeterinarySettingsResponse validResponse;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();
        validResponse = UpdateVeterinarySettingsResponse.builder()
                .id(veterinaryId)
                .timezone("America/Bogota")
                .defaultAppointmentMinutes(45)
                .build();
    }

    @Test
    void updateSettings_shouldReturn200_whenRequestIsValid() throws Exception {
        when(updateVeterinarySettingsUseCase.updateSettings(any(UUID.class), any(UpdateVeterinarySettingsRequest.class)))
                .thenReturn(validResponse);

        mockMvc.perform(patch("/api/veterinary/{veterinaryId}/settings", veterinaryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                UpdateVeterinarySettingsRequest.builder().defaultAppointmentMinutes(45).build()
                        ))
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaultAppointmentMinutes").value(45));
    }

    @Test
    void updateSettings_shouldReturn404_whenVeterinaryNotFound() throws Exception {
        when(updateVeterinarySettingsUseCase.updateSettings(any(UUID.class), any(UpdateVeterinarySettingsRequest.class)))
                .thenThrow(new VeterinaryNotFoundException());

        mockMvc.perform(patch("/api/veterinary/{veterinaryId}/settings", veterinaryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(UpdateVeterinarySettingsRequest.builder().build()))
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateSettings_shouldReturn403_whenCallerLacksClinicConfigure() throws Exception {
        when(updateVeterinarySettingsUseCase.updateSettings(any(UUID.class), any(UpdateVeterinarySettingsRequest.class)))
                .thenThrow(new InsufficientPermissionException(Permission.CLINIC_CONFIGURE));

        mockMvc.perform(patch("/api/veterinary/{veterinaryId}/settings", veterinaryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(UpdateVeterinarySettingsRequest.builder().build()))
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateSettings_shouldReturn401_whenNoTokenIsPresent() throws Exception {
        mockMvc.perform(patch("/api/veterinary/{veterinaryId}/settings", veterinaryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(UpdateVeterinarySettingsRequest.builder().build()))
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}

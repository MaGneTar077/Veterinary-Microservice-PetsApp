package com.MyAnimaLog.Veterinary.infrastructure.professional.controllers;

import com.MyAnimaLog.Veterinary.application.professional.dto.ProfessionalProfileResponse;
import com.MyAnimaLog.Veterinary.application.professional.dto.UpsertProfessionalProfileRequest;
import com.MyAnimaLog.Veterinary.application.professional.ports.in.UpsertProfessionalProfileUseCase;
import com.MyAnimaLog.Veterinary.domain.professional.exceptions.LicenseNumberAlreadyExistsException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VerificationLockedException;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UpsertProfessionalProfileController.class)
@Import(GlobalExceptionHandler.class)
@ImportSecurityConfig
class UpsertProfessionalProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UpsertProfessionalProfileUseCase upsertProfessionalProfileUseCase;

    private UpsertProfessionalProfileRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = UpsertProfessionalProfileRequest.builder()
                .fullName("Juan Pérez")
                .documentType("CC")
                .documentNumber("123456789")
                .profession("MV")
                .licenseNumber("LIC-001")
                .build();
    }

    @Test
    void upsert_shouldReturn200_whenValid() throws Exception {
        when(upsertProfessionalProfileUseCase.upsert(any())).thenReturn(
                ProfessionalProfileResponse.builder().fullName("Juan Pérez").build());

        mockMvc.perform(put("/api/veterinary/me/professional-profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest))
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Juan Pérez"));
    }

    @Test
    void upsert_shouldReturn409_whenLicenseNumberIsTaken() throws Exception {
        when(upsertProfessionalProfileUseCase.upsert(any())).thenThrow(new LicenseNumberAlreadyExistsException());

        mockMvc.perform(put("/api/veterinary/me/professional-profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest))
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LICENSE_NUMBER_TAKEN"));
    }

    @Test
    void upsert_shouldReturn409_whenProfileIsPending() throws Exception {
        when(upsertProfessionalProfileUseCase.upsert(any())).thenThrow(new VerificationLockedException());

        mockMvc.perform(put("/api/veterinary/me/professional-profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest))
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VERIFICATION_LOCKED"));
    }

    @Test
    void upsert_shouldReturn401_whenNoTokenIsPresent() throws Exception {
        mockMvc.perform(put("/api/veterinary/me/professional-profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest))
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}

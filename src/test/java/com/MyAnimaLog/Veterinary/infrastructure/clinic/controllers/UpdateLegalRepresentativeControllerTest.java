package com.MyAnimaLog.Veterinary.infrastructure.clinic.controllers;

import com.MyAnimaLog.Veterinary.application.clinic.dto.UpdateLegalRepresentativeRequest;
import com.MyAnimaLog.Veterinary.application.clinic.dto.UpdateLegalRepresentativeResponse;
import com.MyAnimaLog.Veterinary.application.clinic.ports.in.UpdateLegalRepresentativeUseCase;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.NotVeterinaryOwnerException;
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

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UpdateLegalRepresentativeController.class)
@Import(GlobalExceptionHandler.class)
@ImportSecurityConfig
class UpdateLegalRepresentativeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UpdateLegalRepresentativeUseCase updateLegalRepresentativeUseCase;

    private UUID veterinaryId;
    private UpdateLegalRepresentativeRequest validRequest;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();
        validRequest = UpdateLegalRepresentativeRequest.builder()
                .isOwner(true)
                .fullName("Juan Pérez")
                .documentType("CC")
                .documentNumber("123456789")
                .build();
    }

    @Test
    void update_shouldReturn200_whenCallerIsOwner() throws Exception {
        when(updateLegalRepresentativeUseCase.updateLegalRepresentative(eq(veterinaryId), any()))
                .thenReturn(UpdateLegalRepresentativeResponse.builder()
                        .veterinaryId(veterinaryId)
                        .isOwner(true)
                        .fullName("Juan Pérez")
                        .documentType("CC")
                        .documentNumber("123456789")
                        .build());

        mockMvc.perform(put("/api/veterinary/{veterinaryId}/legal-representative", veterinaryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest))
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Juan Pérez"));
    }

    @Test
    void update_shouldReturn403_whenCallerIsNotOwner() throws Exception {
        when(updateLegalRepresentativeUseCase.updateLegalRepresentative(eq(veterinaryId), any()))
                .thenThrow(new NotVeterinaryOwnerException());

        mockMvc.perform(put("/api/veterinary/{veterinaryId}/legal-representative", veterinaryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest))
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_VETERINARY_OWNER"));
    }

    @Test
    void update_shouldReturn409_whenVerificationIsLocked() throws Exception {
        when(updateLegalRepresentativeUseCase.updateLegalRepresentative(eq(veterinaryId), any()))
                .thenThrow(new VerificationLockedException());

        mockMvc.perform(put("/api/veterinary/{veterinaryId}/legal-representative", veterinaryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest))
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VERIFICATION_LOCKED"));
    }

    @Test
    void update_shouldReturn401_whenNoTokenIsPresent() throws Exception {
        mockMvc.perform(put("/api/veterinary/{veterinaryId}/legal-representative", veterinaryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest))
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}

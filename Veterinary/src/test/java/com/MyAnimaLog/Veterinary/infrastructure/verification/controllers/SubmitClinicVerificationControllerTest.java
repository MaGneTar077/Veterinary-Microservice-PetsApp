package com.MyAnimaLog.Veterinary.infrastructure.verification.controllers;

import com.MyAnimaLog.Veterinary.application.verification.dto.SubmitClinicVerificationRequest;
import com.MyAnimaLog.Veterinary.application.verification.dto.SubmitClinicVerificationResponse;
import com.MyAnimaLog.Veterinary.application.verification.ports.in.SubmitClinicVerificationUseCase;
import com.MyAnimaLog.Veterinary.domain.verification.enums.VerificationRequestStatus;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.MissingRequiredDocumentsException;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.TruthfulnessDeclarationRequiredException;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = SubmitClinicVerificationController.class)
@Import(GlobalExceptionHandler.class)
@ImportSecurityConfig
class SubmitClinicVerificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SubmitClinicVerificationUseCase submitClinicVerificationUseCase;

    private UUID veterinaryId;
    private SubmitClinicVerificationRequest validRequest;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();
        validRequest = SubmitClinicVerificationRequest.builder().truthfulnessDeclared(true).build();
    }

    @Test
    void submit_shouldReturn200_whenValid() throws Exception {
        when(submitClinicVerificationUseCase.submit(eq(veterinaryId), any())).thenReturn(
                SubmitClinicVerificationResponse.builder()
                        .veterinaryId(veterinaryId)
                        .requestId(UUID.randomUUID())
                        .status(VerificationRequestStatus.UNDER_REVIEW)
                        .submittedAt(LocalDateTime.now())
                        .build());

        mockMvc.perform(post("/api/veterinary/{veterinaryId}/verification/submit", veterinaryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest))
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNDER_REVIEW"));
    }

    @Test
    void submit_shouldReturn400_whenTruthfulnessNotDeclared() throws Exception {
        when(submitClinicVerificationUseCase.submit(eq(veterinaryId), any()))
                .thenThrow(new TruthfulnessDeclarationRequiredException());

        mockMvc.perform(post("/api/veterinary/{veterinaryId}/verification/submit", veterinaryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest))
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TRUTHFULNESS_DECLARATION_REQUIRED"));
    }

    @Test
    void submit_shouldReturn400_whenMissingRequiredDocuments() throws Exception {
        when(submitClinicVerificationUseCase.submit(eq(veterinaryId), any()))
                .thenThrow(new MissingRequiredDocumentsException("Missing required: RUT"));

        mockMvc.perform(post("/api/veterinary/{veterinaryId}/verification/submit", veterinaryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest))
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MISSING_REQUIRED_DOCUMENTS"));
    }

    @Test
    void submit_shouldReturn401_whenNoTokenIsPresent() throws Exception {
        mockMvc.perform(post("/api/veterinary/{veterinaryId}/verification/submit", veterinaryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest))
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}

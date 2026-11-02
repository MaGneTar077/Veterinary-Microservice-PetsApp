package com.MyAnimaLog.Veterinary.infrastructure.verification.controllers;

import com.MyAnimaLog.Veterinary.application.verification.dto.RequestVerificationCorrectionResponse;
import com.MyAnimaLog.Veterinary.application.verification.ports.in.RequestVerificationCorrectionUseCase;
import com.MyAnimaLog.Veterinary.domain.verification.enums.SubjectType;
import com.MyAnimaLog.Veterinary.domain.verification.enums.VerificationRequestStatus;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.InvalidDocumentTypeException;
import com.MyAnimaLog.Veterinary.infrastructure.config.GlobalExceptionHandler;
import com.MyAnimaLog.Veterinary.infrastructure.security.ImportSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = RequestVerificationCorrectionController.class)
@Import(GlobalExceptionHandler.class)
@ImportSecurityConfig
class RequestVerificationCorrectionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RequestVerificationCorrectionUseCase requestVerificationCorrectionUseCase;

    @Test
    void requestCorrection_shouldReturn200_whenCallerIsPlatformAdmin() throws Exception {
        UUID requestId = UUID.randomUUID();
        when(requestVerificationCorrectionUseCase.requestCorrection(any(UUID.class), any())).thenReturn(
                RequestVerificationCorrectionResponse.builder().requestId(requestId).subjectType(SubjectType.CLINIC)
                        .status(VerificationRequestStatus.NEEDS_CORRECTION).correctionDocumentTypes(List.of("RUT")).build());

        mockMvc.perform(post("/api/veterinary/admin/verifications/{requestId}/request-correction", requestId)
                        .with(csrf())
                        .with(jwt().authorities(new SimpleGrantedAuthority("PLATFORM_ADMIN")))
                        .contentType("application/json")
                        .content("{\"notes\":\"Falta el RUT vigente\",\"documentTypes\":[\"RUT\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NEEDS_CORRECTION"));
    }

    @Test
    void requestCorrection_shouldReturn400_whenDocumentTypeInvalidForSubject() throws Exception {
        UUID requestId = UUID.randomUUID();
        when(requestVerificationCorrectionUseCase.requestCorrection(any(UUID.class), any()))
                .thenThrow(new InvalidDocumentTypeException("bad type"));

        mockMvc.perform(post("/api/veterinary/admin/verifications/{requestId}/request-correction", requestId)
                        .with(csrf())
                        .with(jwt().authorities(new SimpleGrantedAuthority("PLATFORM_ADMIN")))
                        .contentType("application/json")
                        .content("{\"notes\":\"x\",\"documentTypes\":[\"PROFESSIONAL_CARD\"]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requestCorrection_shouldReturn403_whenCallerLacksPlatformAdminAuthority() throws Exception {
        mockMvc.perform(post("/api/veterinary/admin/verifications/{requestId}/request-correction", UUID.randomUUID())
                        .with(csrf())
                        .with(jwt())
                        .contentType("application/json")
                        .content("{\"notes\":\"x\",\"documentTypes\":[\"RUT\"]}"))
                .andExpect(status().isForbidden());
    }
}

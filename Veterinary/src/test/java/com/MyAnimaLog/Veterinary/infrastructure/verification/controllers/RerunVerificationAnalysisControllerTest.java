package com.MyAnimaLog.Veterinary.infrastructure.verification.controllers;

import com.MyAnimaLog.Veterinary.application.verification.dto.RerunVerificationAnalysisResponse;
import com.MyAnimaLog.Veterinary.application.verification.ports.in.RerunVerificationAnalysisUseCase;
import com.MyAnimaLog.Veterinary.domain.verification.enums.AiStatus;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.AiVerificationDisabledException;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.VerificationRequestNotFoundException;
import com.MyAnimaLog.Veterinary.infrastructure.config.GlobalExceptionHandler;
import com.MyAnimaLog.Veterinary.infrastructure.security.ImportSecurityConfig;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = RerunVerificationAnalysisController.class)
@Import(GlobalExceptionHandler.class)
@ImportSecurityConfig
class RerunVerificationAnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RerunVerificationAnalysisUseCase rerunVerificationAnalysisUseCase;

    @Test
    void rerun_shouldReturn200_whenCallerIsPlatformAdmin() throws Exception {
        UUID requestId = UUID.randomUUID();
        when(rerunVerificationAnalysisUseCase.rerun(any(UUID.class))).thenReturn(
                RerunVerificationAnalysisResponse.builder().requestId(requestId).aiStatus(AiStatus.DONE).build());

        mockMvc.perform(post("/api/veterinary/admin/verifications/{requestId}/rerun-ai", requestId)
                        .with(csrf())
                        .with(jwt().authorities(new SimpleGrantedAuthority("PLATFORM_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aiStatus").value("DONE"));
    }

    @Test
    void rerun_shouldReturn409_whenAiDisabled() throws Exception {
        UUID requestId = UUID.randomUUID();
        when(rerunVerificationAnalysisUseCase.rerun(any(UUID.class))).thenThrow(new AiVerificationDisabledException());

        mockMvc.perform(post("/api/veterinary/admin/verifications/{requestId}/rerun-ai", requestId)
                        .with(csrf())
                        .with(jwt().authorities(new SimpleGrantedAuthority("PLATFORM_ADMIN"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("AI_VERIFICATION_DISABLED"));
    }

    @Test
    void rerun_shouldReturn404_whenRequestNotFound() throws Exception {
        UUID requestId = UUID.randomUUID();
        when(rerunVerificationAnalysisUseCase.rerun(any(UUID.class))).thenThrow(new VerificationRequestNotFoundException());

        mockMvc.perform(post("/api/veterinary/admin/verifications/{requestId}/rerun-ai", requestId)
                        .with(csrf())
                        .with(jwt().authorities(new SimpleGrantedAuthority("PLATFORM_ADMIN"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void rerun_shouldReturn403_whenCallerLacksPlatformAdminAuthority() throws Exception {
        mockMvc.perform(post("/api/veterinary/admin/verifications/{requestId}/rerun-ai", UUID.randomUUID())
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isForbidden());
    }
}

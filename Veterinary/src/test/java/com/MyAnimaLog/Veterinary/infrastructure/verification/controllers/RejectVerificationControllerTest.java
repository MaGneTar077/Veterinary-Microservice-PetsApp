package com.MyAnimaLog.Veterinary.infrastructure.verification.controllers;

import com.MyAnimaLog.Veterinary.application.verification.dto.RejectVerificationResponse;
import com.MyAnimaLog.Veterinary.application.verification.ports.in.RejectVerificationUseCase;
import com.MyAnimaLog.Veterinary.domain.verification.enums.SubjectType;
import com.MyAnimaLog.Veterinary.domain.verification.enums.VerificationRequestStatus;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.InvalidReviewDecisionException;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.InvalidVerificationStateException;
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

@WebMvcTest(controllers = RejectVerificationController.class)
@Import(GlobalExceptionHandler.class)
@ImportSecurityConfig
class RejectVerificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RejectVerificationUseCase rejectVerificationUseCase;

    @Test
    void reject_shouldReturn200_whenCallerIsPlatformAdmin() throws Exception {
        UUID requestId = UUID.randomUUID();
        when(rejectVerificationUseCase.reject(any(UUID.class), any())).thenReturn(
                RejectVerificationResponse.builder().requestId(requestId).subjectType(SubjectType.CLINIC)
                        .status(VerificationRequestStatus.REJECTED).attemptsRemaining(2).build());

        mockMvc.perform(post("/api/veterinary/admin/verifications/{requestId}/reject", requestId)
                        .with(csrf())
                        .with(jwt().authorities(new SimpleGrantedAuthority("PLATFORM_ADMIN")))
                        .contentType("application/json")
                        .content("{\"reasonCode\":\"NOT_IN_REGISTRY\",\"notes\":\"No aparece en RUES\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attemptsRemaining").value(2));
    }

    @Test
    void reject_shouldReturn400_whenReasonCodeMissing() throws Exception {
        UUID requestId = UUID.randomUUID();
        when(rejectVerificationUseCase.reject(any(UUID.class), any())).thenThrow(new InvalidReviewDecisionException());

        mockMvc.perform(post("/api/veterinary/admin/verifications/{requestId}/reject", requestId)
                        .with(csrf())
                        .with(jwt().authorities(new SimpleGrantedAuthority("PLATFORM_ADMIN")))
                        .contentType("application/json")
                        .content("{\"notes\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void reject_shouldReturn409_whenNotUnderReview() throws Exception {
        UUID requestId = UUID.randomUUID();
        when(rejectVerificationUseCase.reject(any(UUID.class), any())).thenThrow(new InvalidVerificationStateException());

        mockMvc.perform(post("/api/veterinary/admin/verifications/{requestId}/reject", requestId)
                        .with(csrf())
                        .with(jwt().authorities(new SimpleGrantedAuthority("PLATFORM_ADMIN")))
                        .contentType("application/json")
                        .content("{\"reasonCode\":\"OTHER\",\"notes\":\"x\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void reject_shouldReturn403_whenCallerLacksPlatformAdminAuthority() throws Exception {
        mockMvc.perform(post("/api/veterinary/admin/verifications/{requestId}/reject", UUID.randomUUID())
                        .with(csrf())
                        .with(jwt())
                        .contentType("application/json")
                        .content("{\"reasonCode\":\"OTHER\",\"notes\":\"x\"}"))
                .andExpect(status().isForbidden());
    }
}

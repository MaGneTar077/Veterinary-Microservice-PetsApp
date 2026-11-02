package com.MyAnimaLog.Veterinary.infrastructure.verification.controllers;

import com.MyAnimaLog.Veterinary.application.verification.dto.RetryClinicVerificationResponse;
import com.MyAnimaLog.Veterinary.application.verification.ports.in.RetryClinicVerificationUseCase;
import com.MyAnimaLog.Veterinary.domain.verification.enums.VerificationRequestStatus;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.VerificationAttemptsExhaustedException;
import com.MyAnimaLog.Veterinary.infrastructure.config.GlobalExceptionHandler;
import com.MyAnimaLog.Veterinary.infrastructure.security.ImportSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = RetryClinicVerificationController.class)
@Import(GlobalExceptionHandler.class)
@ImportSecurityConfig
class RetryClinicVerificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RetryClinicVerificationUseCase retryClinicVerificationUseCase;

    @Test
    void retry_shouldReturn200_whenAllowed() throws Exception {
        UUID veterinaryId = UUID.randomUUID();
        when(retryClinicVerificationUseCase.retry(veterinaryId)).thenReturn(
                RetryClinicVerificationResponse.builder()
                        .veterinaryId(veterinaryId)
                        .requestId(UUID.randomUUID())
                        .status(VerificationRequestStatus.DRAFT)
                        .attemptNumber(2)
                        .build());

        mockMvc.perform(post("/api/veterinary/{veterinaryId}/verification/retry", veterinaryId)
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attemptNumber").value(2));
    }

    @Test
    void retry_shouldReturn409_whenAttemptsExhausted() throws Exception {
        UUID veterinaryId = UUID.randomUUID();
        when(retryClinicVerificationUseCase.retry(veterinaryId)).thenThrow(new VerificationAttemptsExhaustedException());

        mockMvc.perform(post("/api/veterinary/{veterinaryId}/verification/retry", veterinaryId)
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VERIFICATION_ATTEMPTS_EXHAUSTED"));
    }

    @Test
    void retry_shouldReturn401_whenNoTokenIsPresent() throws Exception {
        UUID veterinaryId = UUID.randomUUID();

        mockMvc.perform(post("/api/veterinary/{veterinaryId}/verification/retry", veterinaryId)
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}

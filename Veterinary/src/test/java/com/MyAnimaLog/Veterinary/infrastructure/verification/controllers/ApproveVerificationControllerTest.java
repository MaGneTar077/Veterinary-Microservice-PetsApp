package com.MyAnimaLog.Veterinary.infrastructure.verification.controllers;

import com.MyAnimaLog.Veterinary.application.verification.dto.ApproveVerificationResponse;
import com.MyAnimaLog.Veterinary.application.verification.ports.in.ApproveVerificationUseCase;
import com.MyAnimaLog.Veterinary.domain.verification.enums.SubjectType;
import com.MyAnimaLog.Veterinary.domain.verification.enums.VerificationRequestStatus;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.CannotReviewOwnRequestException;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.RegistryCheckRequiredException;
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

@WebMvcTest(controllers = ApproveVerificationController.class)
@Import(GlobalExceptionHandler.class)
@ImportSecurityConfig
class ApproveVerificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ApproveVerificationUseCase approveVerificationUseCase;

    @Test
    void approve_shouldReturn200_whenCallerIsPlatformAdmin() throws Exception {
        UUID requestId = UUID.randomUUID();
        when(approveVerificationUseCase.approve(any(UUID.class), any())).thenReturn(
                ApproveVerificationResponse.builder().requestId(requestId).subjectType(SubjectType.CLINIC)
                        .status(VerificationRequestStatus.APPROVED).build());

        mockMvc.perform(post("/api/veterinary/admin/verifications/{requestId}/approve", requestId)
                        .with(csrf())
                        .with(jwt().authorities(new SimpleGrantedAuthority("PLATFORM_ADMIN")))
                        .contentType("application/json")
                        .content("{\"registryChecked\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void approve_shouldReturn400_whenRegistryCheckMissing() throws Exception {
        UUID requestId = UUID.randomUUID();
        when(approveVerificationUseCase.approve(any(UUID.class), any())).thenThrow(new RegistryCheckRequiredException());

        mockMvc.perform(post("/api/veterinary/admin/verifications/{requestId}/approve", requestId)
                        .with(csrf())
                        .with(jwt().authorities(new SimpleGrantedAuthority("PLATFORM_ADMIN")))
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REGISTRY_CHECK_REQUIRED"));
    }

    @Test
    void approve_shouldReturn403_whenAdminSubmittedTheirOwnRequest() throws Exception {
        UUID requestId = UUID.randomUUID();
        when(approveVerificationUseCase.approve(any(UUID.class), any())).thenThrow(new CannotReviewOwnRequestException());

        mockMvc.perform(post("/api/veterinary/admin/verifications/{requestId}/approve", requestId)
                        .with(csrf())
                        .with(jwt().authorities(new SimpleGrantedAuthority("PLATFORM_ADMIN")))
                        .contentType("application/json")
                        .content("{\"registryChecked\":true}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CANNOT_REVIEW_OWN_REQUEST"));
    }

    @Test
    void approve_shouldReturn403_whenCallerLacksPlatformAdminAuthority() throws Exception {
        mockMvc.perform(post("/api/veterinary/admin/verifications/{requestId}/approve", UUID.randomUUID())
                        .with(csrf())
                        .with(jwt())
                        .contentType("application/json")
                        .content("{\"registryChecked\":true}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void approve_shouldReturn401_whenNoTokenIsPresent() throws Exception {
        mockMvc.perform(post("/api/veterinary/admin/verifications/{requestId}/approve", UUID.randomUUID())
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"registryChecked\":true}"))
                .andExpect(status().isUnauthorized());
    }
}

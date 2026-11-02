package com.MyAnimaLog.Veterinary.infrastructure.verification.controllers;

import com.MyAnimaLog.Veterinary.application.verification.dto.AdminVerificationDetailResponse;
import com.MyAnimaLog.Veterinary.application.verification.ports.in.GetAdminVerificationDetailUseCase;
import com.MyAnimaLog.Veterinary.domain.verification.enums.SubjectType;
import com.MyAnimaLog.Veterinary.domain.verification.enums.VerificationRequestStatus;
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

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = GetAdminVerificationDetailController.class)
@Import(GlobalExceptionHandler.class)
@ImportSecurityConfig
class GetAdminVerificationDetailControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetAdminVerificationDetailUseCase getAdminVerificationDetailUseCase;

    @Test
    void getDetail_shouldReturn200_whenCallerIsPlatformAdmin() throws Exception {
        UUID requestId = UUID.randomUUID();
        when(getAdminVerificationDetailUseCase.getDetail(requestId)).thenReturn(
                AdminVerificationDetailResponse.builder()
                        .requestId(requestId)
                        .subjectType(SubjectType.CLINIC)
                        .status(VerificationRequestStatus.UNDER_REVIEW)
                        .build());

        mockMvc.perform(get("/api/veterinary/admin/verifications/{requestId}", requestId)
                        .with(csrf())
                        .with(jwt().authorities(new SimpleGrantedAuthority("PLATFORM_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subjectType").value("CLINIC"));
    }

    @Test
    void getDetail_shouldReturn404_whenRequestNotFound() throws Exception {
        UUID requestId = UUID.randomUUID();
        when(getAdminVerificationDetailUseCase.getDetail(requestId)).thenThrow(new VerificationRequestNotFoundException());

        mockMvc.perform(get("/api/veterinary/admin/verifications/{requestId}", requestId)
                        .with(csrf())
                        .with(jwt().authorities(new SimpleGrantedAuthority("PLATFORM_ADMIN"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void getDetail_shouldReturn403_whenCallerLacksPlatformAdminAuthority() throws Exception {
        mockMvc.perform(get("/api/veterinary/admin/verifications/{requestId}", UUID.randomUUID())
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void getDetail_shouldReturn401_whenNoTokenIsPresent() throws Exception {
        mockMvc.perform(get("/api/veterinary/admin/verifications/{requestId}", UUID.randomUUID())
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}

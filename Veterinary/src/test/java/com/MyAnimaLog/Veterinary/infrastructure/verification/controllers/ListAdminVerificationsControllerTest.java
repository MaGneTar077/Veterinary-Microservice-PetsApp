package com.MyAnimaLog.Veterinary.infrastructure.verification.controllers;

import com.MyAnimaLog.Veterinary.application.verification.dto.AdminVerificationQueueResponse;
import com.MyAnimaLog.Veterinary.application.verification.ports.in.ListAdminVerificationsUseCase;
import com.MyAnimaLog.Veterinary.domain.verification.enums.SubjectType;
import com.MyAnimaLog.Veterinary.domain.verification.enums.VerificationRequestStatus;
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
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ListAdminVerificationsController.class)
@Import(GlobalExceptionHandler.class)
@ImportSecurityConfig
class ListAdminVerificationsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListAdminVerificationsUseCase listAdminVerificationsUseCase;

    @Test
    void list_shouldReturn200_whenCallerIsPlatformAdmin() throws Exception {
        when(listAdminVerificationsUseCase.list(isNull(), isNull(), isNull(), isNull())).thenReturn(
                AdminVerificationQueueResponse.builder()
                        .items(List.of())
                        .page(0).size(20).total(0)
                        .counts(Map.of("UNDER_REVIEW", 0L))
                        .build());

        mockMvc.perform(get("/api/veterinary/admin/verifications")
                        .with(csrf())
                        .with(jwt().authorities(new SimpleGrantedAuthority("PLATFORM_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0));
    }

    @Test
    void list_shouldPassFiltersThrough() throws Exception {
        when(listAdminVerificationsUseCase.list(eq(VerificationRequestStatus.REJECTED), eq(SubjectType.CLINIC), eq(1), eq(10)))
                .thenReturn(AdminVerificationQueueResponse.builder().items(List.of()).page(1).size(10).total(0).counts(Map.of()).build());

        mockMvc.perform(get("/api/veterinary/admin/verifications")
                        .param("status", "REJECTED")
                        .param("subjectType", "CLINIC")
                        .param("page", "1")
                        .param("size", "10")
                        .with(csrf())
                        .with(jwt().authorities(new SimpleGrantedAuthority("PLATFORM_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(10));
    }

    @Test
    void list_shouldReturn403_whenCallerLacksPlatformAdminAuthority() throws Exception {
        mockMvc.perform(get("/api/veterinary/admin/verifications")
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void list_shouldReturn401_whenNoTokenIsPresent() throws Exception {
        mockMvc.perform(get("/api/veterinary/admin/verifications")
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}

package com.MyAnimaLog.Veterinary.infrastructure.verification.controllers;

import com.MyAnimaLog.Veterinary.application.verification.dto.RenewSignedUrlResponse;
import com.MyAnimaLog.Veterinary.application.verification.ports.in.RenewVerificationDocumentSignedUrlUseCase;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.VerificationDocumentNotFoundException;
import com.MyAnimaLog.Veterinary.infrastructure.config.GlobalExceptionHandler;
import com.MyAnimaLog.Veterinary.infrastructure.security.ImportSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = RenewVerificationDocumentSignedUrlController.class)
@Import(GlobalExceptionHandler.class)
@ImportSecurityConfig
class RenewVerificationDocumentSignedUrlControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RenewVerificationDocumentSignedUrlUseCase renewVerificationDocumentSignedUrlUseCase;

    @Test
    void renew_shouldReturn200_whenCallerIsPlatformAdmin() throws Exception {
        UUID requestId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        when(renewVerificationDocumentSignedUrlUseCase.renew(requestId, documentId)).thenReturn(
                RenewSignedUrlResponse.builder().signedUrl("https://signed.example/doc.pdf").expiresAt(LocalDateTime.now().plusMinutes(10)).build());

        mockMvc.perform(get("/api/veterinary/admin/verifications/{requestId}/documents/{documentId}/signed-url", requestId, documentId)
                        .with(csrf())
                        .with(jwt().authorities(new SimpleGrantedAuthority("PLATFORM_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.signedUrl").value("https://signed.example/doc.pdf"));
    }

    @Test
    void renew_shouldReturn404_whenDocumentNotFound() throws Exception {
        UUID requestId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        when(renewVerificationDocumentSignedUrlUseCase.renew(requestId, documentId)).thenThrow(new VerificationDocumentNotFoundException());

        mockMvc.perform(get("/api/veterinary/admin/verifications/{requestId}/documents/{documentId}/signed-url", requestId, documentId)
                        .with(csrf())
                        .with(jwt().authorities(new SimpleGrantedAuthority("PLATFORM_ADMIN"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void renew_shouldReturn403_whenCallerLacksPlatformAdminAuthority() throws Exception {
        mockMvc.perform(get("/api/veterinary/admin/verifications/{requestId}/documents/{documentId}/signed-url", UUID.randomUUID(), UUID.randomUUID())
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isForbidden());
    }
}

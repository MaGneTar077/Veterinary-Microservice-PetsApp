package com.MyAnimaLog.Veterinary.infrastructure.verification.controllers;

import com.MyAnimaLog.Veterinary.application.verification.dto.CleanupVerificationDocumentsResponse;
import com.MyAnimaLog.Veterinary.application.verification.ports.in.CleanupVerificationDocumentsJobUseCase;
import com.MyAnimaLog.Veterinary.infrastructure.config.GlobalExceptionHandler;
import com.MyAnimaLog.Veterinary.infrastructure.security.ImportSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CleanupVerificationDocumentsJobController.class)
@Import(GlobalExceptionHandler.class)
@ImportSecurityConfig
class CleanupVerificationDocumentsJobControllerTest {

    private static final String HEADER = "X-Internal-Api-Key";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CleanupVerificationDocumentsJobUseCase cleanupVerificationDocumentsJobUseCase;

    @Value("${app.security.internal-api-key}")
    private String internalApiKey;

    @Test
    void cleanup_shouldReturn401_whenInternalApiKeyIsMissing() throws Exception {
        mockMvc.perform(post("/internal/jobs/verification-documents/cleanup"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cleanup_shouldReturn200_whenInternalApiKeyIsCorrect() throws Exception {
        when(cleanupVerificationDocumentsJobUseCase.cleanup()).thenReturn(
                CleanupVerificationDocumentsResponse.builder().affectedRequests(2).deletedDocuments(5).build());

        mockMvc.perform(post("/internal/jobs/verification-documents/cleanup")
                        .header(HEADER, internalApiKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.affectedRequests").value(2))
                .andExpect(jsonPath("$.deletedDocuments").value(5));
    }
}

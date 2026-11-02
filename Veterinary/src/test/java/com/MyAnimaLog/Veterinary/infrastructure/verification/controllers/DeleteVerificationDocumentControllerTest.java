package com.MyAnimaLog.Veterinary.infrastructure.verification.controllers;

import com.MyAnimaLog.Veterinary.application.verification.ports.in.DeleteVerificationDocumentUseCase;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.NotVeterinaryOwnerException;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.VerificationDocumentNotFoundException;
import com.MyAnimaLog.Veterinary.infrastructure.config.GlobalExceptionHandler;
import com.MyAnimaLog.Veterinary.infrastructure.security.ImportSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DeleteVerificationDocumentController.class)
@Import(GlobalExceptionHandler.class)
@ImportSecurityConfig
class DeleteVerificationDocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DeleteVerificationDocumentUseCase deleteVerificationDocumentUseCase;

    @Test
    void delete_shouldReturn204_whenCallerIsOwner() throws Exception {
        UUID veterinaryId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();

        mockMvc.perform(delete("/api/veterinary/{veterinaryId}/verification/documents/{documentId}", veterinaryId, documentId)
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_shouldReturn404_whenDocumentNotFound() throws Exception {
        UUID veterinaryId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        doThrow(new VerificationDocumentNotFoundException())
                .when(deleteVerificationDocumentUseCase).deleteDocument(veterinaryId, documentId);

        mockMvc.perform(delete("/api/veterinary/{veterinaryId}/verification/documents/{documentId}", veterinaryId, documentId)
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_shouldReturn403_whenCallerIsNotOwner() throws Exception {
        UUID veterinaryId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        doThrow(new NotVeterinaryOwnerException())
                .when(deleteVerificationDocumentUseCase).deleteDocument(veterinaryId, documentId);

        mockMvc.perform(delete("/api/veterinary/{veterinaryId}/verification/documents/{documentId}", veterinaryId, documentId)
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_shouldReturn401_whenNoTokenIsPresent() throws Exception {
        UUID veterinaryId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();

        mockMvc.perform(delete("/api/veterinary/{veterinaryId}/verification/documents/{documentId}", veterinaryId, documentId)
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}

package com.MyAnimaLog.Veterinary.infrastructure.verification.controllers;

import com.MyAnimaLog.Veterinary.application.verification.dto.UploadVerificationDocumentResponse;
import com.MyAnimaLog.Veterinary.application.verification.ports.in.UploadVerificationDocumentUseCase;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.NotVeterinaryOwnerException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VerificationLockedException;
import com.MyAnimaLog.Veterinary.infrastructure.config.GlobalExceptionHandler;
import com.MyAnimaLog.Veterinary.infrastructure.security.ImportSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UploadVerificationDocumentController.class)
@Import(GlobalExceptionHandler.class)
@ImportSecurityConfig
class UploadVerificationDocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UploadVerificationDocumentUseCase uploadVerificationDocumentUseCase;

    @Test
    void upload_shouldReturn201_whenCallerIsOwner() throws Exception {
        UUID veterinaryId = UUID.randomUUID();
        when(uploadVerificationDocumentUseCase.uploadDocument(eq(veterinaryId), eq("RUT"), any(), any()))
                .thenReturn(UploadVerificationDocumentResponse.builder()
                        .documentId(UUID.randomUUID())
                        .documentType("RUT")
                        .fileName("abc.pdf")
                        .build());

        MockMultipartFile file = new MockMultipartFile("file", "rut.pdf", "application/pdf", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/veterinary/{veterinaryId}/verification/documents", veterinaryId)
                        .file(file)
                        .param("documentType", "RUT")
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.documentType").value("RUT"));
    }

    @Test
    void upload_shouldReturn403_whenCallerIsNotOwner() throws Exception {
        UUID veterinaryId = UUID.randomUUID();
        when(uploadVerificationDocumentUseCase.uploadDocument(eq(veterinaryId), eq("RUT"), any(), any()))
                .thenThrow(new NotVeterinaryOwnerException());

        MockMultipartFile file = new MockMultipartFile("file", "rut.pdf", "application/pdf", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/veterinary/{veterinaryId}/verification/documents", veterinaryId)
                        .file(file)
                        .param("documentType", "RUT")
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void upload_shouldReturn409_whenVerificationIsLocked() throws Exception {
        UUID veterinaryId = UUID.randomUUID();
        when(uploadVerificationDocumentUseCase.uploadDocument(eq(veterinaryId), eq("RUT"), any(), any()))
                .thenThrow(new VerificationLockedException());

        MockMultipartFile file = new MockMultipartFile("file", "rut.pdf", "application/pdf", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/veterinary/{veterinaryId}/verification/documents", veterinaryId)
                        .file(file)
                        .param("documentType", "RUT")
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isConflict());
    }

    @Test
    void upload_shouldReturn401_whenNoTokenIsPresent() throws Exception {
        UUID veterinaryId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "rut.pdf", "application/pdf", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/veterinary/{veterinaryId}/verification/documents", veterinaryId)
                        .file(file)
                        .param("documentType", "RUT")
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}

package com.MyAnimaLog.Veterinary.infrastructure.professional.controllers;

import com.MyAnimaLog.Veterinary.application.professional.dto.UploadProfessionalDocumentResponse;
import com.MyAnimaLog.Veterinary.application.professional.ports.in.UploadProfessionalDocumentUseCase;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.InvalidFileTypeException;
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

@WebMvcTest(controllers = UploadProfessionalDocumentController.class)
@Import(GlobalExceptionHandler.class)
@ImportSecurityConfig
class UploadProfessionalDocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UploadProfessionalDocumentUseCase uploadProfessionalDocumentUseCase;

    @Test
    void upload_shouldReturn201_whenValid() throws Exception {
        when(uploadProfessionalDocumentUseCase.uploadDocument(eq("PROFESSIONAL_CARD"), any(), any()))
                .thenReturn(UploadProfessionalDocumentResponse.builder()
                        .documentId(UUID.randomUUID())
                        .documentType("PROFESSIONAL_CARD")
                        .fileName("tarjeta.pdf")
                        .build());

        MockMultipartFile file = new MockMultipartFile("file", "tarjeta.pdf", "application/pdf", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/veterinary/me/professional-profile/documents")
                        .file(file)
                        .param("documentType", "PROFESSIONAL_CARD")
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.documentType").value("PROFESSIONAL_CARD"));
    }

    @Test
    void upload_shouldReturn400_whenFileTypeIsInvalid() throws Exception {
        when(uploadProfessionalDocumentUseCase.uploadDocument(eq("PROFESSIONAL_CARD"), any(), any()))
                .thenThrow(new InvalidFileTypeException());

        MockMultipartFile file = new MockMultipartFile("file", "tarjeta.pdf", "application/pdf", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/veterinary/me/professional-profile/documents")
                        .file(file)
                        .param("documentType", "PROFESSIONAL_CARD")
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_FILE_TYPE"));
    }

    @Test
    void upload_shouldReturn409_whenVerificationIsLocked() throws Exception {
        when(uploadProfessionalDocumentUseCase.uploadDocument(eq("PROFESSIONAL_CARD"), any(), any()))
                .thenThrow(new VerificationLockedException());

        MockMultipartFile file = new MockMultipartFile("file", "tarjeta.pdf", "application/pdf", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/veterinary/me/professional-profile/documents")
                        .file(file)
                        .param("documentType", "PROFESSIONAL_CARD")
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isConflict());
    }

    @Test
    void upload_shouldReturn401_whenNoTokenIsPresent() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "tarjeta.pdf", "application/pdf", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/veterinary/me/professional-profile/documents")
                        .file(file)
                        .param("documentType", "PROFESSIONAL_CARD")
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}

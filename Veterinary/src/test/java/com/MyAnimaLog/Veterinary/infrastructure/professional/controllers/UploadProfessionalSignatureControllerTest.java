package com.MyAnimaLog.Veterinary.infrastructure.professional.controllers;

import com.MyAnimaLog.Veterinary.application.professional.dto.UploadSignatureResponse;
import com.MyAnimaLog.Veterinary.application.professional.ports.in.UploadProfessionalSignatureUseCase;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.InvalidFileTypeException;
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
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UploadProfessionalSignatureController.class)
@Import(GlobalExceptionHandler.class)
@ImportSecurityConfig
class UploadProfessionalSignatureControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UploadProfessionalSignatureUseCase uploadProfessionalSignatureUseCase;

    @Test
    void upload_shouldReturn200_whenValid() throws Exception {
        when(uploadProfessionalSignatureUseCase.uploadSignature(any())).thenReturn(
                UploadSignatureResponse.builder().profileId(UUID.randomUUID()).hasSignature(true).build());

        MockMultipartFile file = new MockMultipartFile("file", "signature.png", "image/png", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/veterinary/me/professional-profile/signature")
                        .file(file)
                        .with(req -> { req.setMethod("PUT"); return req; })
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasSignature").value(true));
    }

    @Test
    void upload_shouldReturn400_whenNotPng() throws Exception {
        when(uploadProfessionalSignatureUseCase.uploadSignature(any())).thenThrow(new InvalidFileTypeException());

        MockMultipartFile file = new MockMultipartFile("file", "signature.pdf", "application/pdf", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/veterinary/me/professional-profile/signature")
                        .file(file)
                        .with(req -> { req.setMethod("PUT"); return req; })
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_FILE_TYPE"));
    }

    @Test
    void upload_shouldReturn401_whenNoTokenIsPresent() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "signature.png", "image/png", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/veterinary/me/professional-profile/signature")
                        .file(file)
                        .with(req -> { req.setMethod("PUT"); return req; })
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}

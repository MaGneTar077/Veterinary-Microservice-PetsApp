package com.MyAnimaLog.Veterinary.infrastructure.clinic.controllers;

import com.MyAnimaLog.Veterinary.application.clinic.dto.UploadLogoResponse;
import com.MyAnimaLog.Veterinary.application.clinic.ports.in.UploadLogoUseCase;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.FileTooLargeException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.InvalidFileTypeException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.NotVeterinaryOwnerException;
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

@WebMvcTest(controllers = UploadLogoController.class)
@Import(GlobalExceptionHandler.class)
@ImportSecurityConfig
class UploadLogoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UploadLogoUseCase uploadLogoUseCase;

    @Test
    void upload_shouldReturn200_whenCallerIsOwner() throws Exception {
        UUID veterinaryId = UUID.randomUUID();
        when(uploadLogoUseCase.uploadLogo(eq(veterinaryId), any(), any()))
                .thenReturn(UploadLogoResponse.builder().veterinaryId(veterinaryId).logoUrl("https://cdn/logo.png").build());

        MockMultipartFile file = new MockMultipartFile("file", "logo.png", "image/png", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/veterinary/{veterinaryId}/logo", veterinaryId)
                        .file(file)
                        .with(req -> { req.setMethod("PUT"); return req; })
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logoUrl").value("https://cdn/logo.png"));
    }

    @Test
    void upload_shouldReturn403_whenCallerIsNotOwnerOrConfigure() throws Exception {
        UUID veterinaryId = UUID.randomUUID();
        when(uploadLogoUseCase.uploadLogo(eq(veterinaryId), any(), any()))
                .thenThrow(new NotVeterinaryOwnerException());

        MockMultipartFile file = new MockMultipartFile("file", "logo.png", "image/png", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/veterinary/{veterinaryId}/logo", veterinaryId)
                        .file(file)
                        .with(req -> { req.setMethod("PUT"); return req; })
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void upload_shouldReturn400_whenFileTypeIsInvalid() throws Exception {
        UUID veterinaryId = UUID.randomUUID();
        when(uploadLogoUseCase.uploadLogo(eq(veterinaryId), any(), any()))
                .thenThrow(new InvalidFileTypeException());

        MockMultipartFile file = new MockMultipartFile("file", "logo.png", "image/png", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/veterinary/{veterinaryId}/logo", veterinaryId)
                        .file(file)
                        .with(req -> { req.setMethod("PUT"); return req; })
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_FILE_TYPE"));
    }

    @Test
    void upload_shouldReturn413_whenFileIsTooLarge() throws Exception {
        UUID veterinaryId = UUID.randomUUID();
        when(uploadLogoUseCase.uploadLogo(eq(veterinaryId), any(), any()))
                .thenThrow(new FileTooLargeException());

        MockMultipartFile file = new MockMultipartFile("file", "logo.png", "image/png", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/veterinary/{veterinaryId}/logo", veterinaryId)
                        .file(file)
                        .with(req -> { req.setMethod("PUT"); return req; })
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.code").value("FILE_TOO_LARGE"));
    }

    @Test
    void upload_shouldReturn401_whenNoTokenIsPresent() throws Exception {
        UUID veterinaryId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "logo.png", "image/png", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/veterinary/{veterinaryId}/logo", veterinaryId)
                        .file(file)
                        .with(req -> { req.setMethod("PUT"); return req; })
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}

package com.MyAnimaLog.Veterinary.infrastructure.patients.controllers;

import com.MyAnimaLog.Veterinary.application.patients.dto.LinkByCodeRequest;
import com.MyAnimaLog.Veterinary.application.patients.dto.LinkByCodeResponse;
import com.MyAnimaLog.Veterinary.application.patients.ports.in.LinkByCodeUseCase;
import com.MyAnimaLog.Veterinary.domain.patients.exceptions.InvalidInviteCodeException;
import com.MyAnimaLog.Veterinary.domain.patients.exceptions.LinkCodeNotFoundException;
import com.MyAnimaLog.Veterinary.infrastructure.config.GlobalExceptionHandler;
import com.MyAnimaLog.Veterinary.infrastructure.security.ImportSecurityConfig;
import com.MyAnimaLog.Veterinary.infrastructure.patients.controllers.LinkByCodeController;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = LinkByCodeController.class)
@Import(GlobalExceptionHandler.class)
@ImportSecurityConfig
class LinkByCodeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LinkByCodeUseCase linkByCodeUseCase;

    private UUID userId;
    private LinkByCodeRequest validRequest;
    private LinkByCodeResponse validResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        validRequest = LinkByCodeRequest.builder()
                .inviteCode("ABCD2345")
                .build();

        validResponse = LinkByCodeResponse.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .veterinaryId(UUID.randomUUID())
                .status("LINKED")
                .linkedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void linkByCode_shouldReturn200_whenRequestIsValid() throws Exception {
        when(linkByCodeUseCase.linkByCode(any(LinkByCodeRequest.class))).thenReturn(validResponse);

        mockMvc.perform(post("/api/veterinary/link/code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest))
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.status").value("LINKED"))
                .andExpect(jsonPath("$.linkedAt").isNotEmpty());
    }

    @Test
    void linkByCode_shouldReturn400_whenInviteCodeIsInvalid() throws Exception {
        when(linkByCodeUseCase.linkByCode(any(LinkByCodeRequest.class)))
                .thenThrow(new InvalidInviteCodeException());

        mockMvc.perform(post("/api/veterinary/link/code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest))
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invite code is invalid or does not exist"));
    }

    @Test
    void linkByCode_shouldReturn404_whenCodeDoesNotResolveToAnActiveClinic() throws Exception {
        // Desde la Fase 4A el alias hereda el contrato nuevo: un código inexistente y una clínica
        // no ACTIVE son el mismo 404 (antes 400 INVALID_INVITE_CODE y 409 VETERINARY_NOT_ACTIVE).
        when(linkByCodeUseCase.linkByCode(any(LinkByCodeRequest.class)))
                .thenThrow(new LinkCodeNotFoundException());

        mockMvc.perform(post("/api/veterinary/link/code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest))
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LINK_CODE_NOT_FOUND"));
    }

    @Test
    void linkByCode_shouldReturn401_whenNoTokenIsPresent() throws Exception {
        mockMvc.perform(post("/api/veterinary/link/code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest))
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}
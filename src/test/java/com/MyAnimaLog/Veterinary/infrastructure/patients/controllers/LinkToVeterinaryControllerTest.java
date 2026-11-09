package com.MyAnimaLog.Veterinary.infrastructure.patients.controllers;

import com.MyAnimaLog.Veterinary.application.patients.dto.LinkToVeterinaryRequest;
import com.MyAnimaLog.Veterinary.application.patients.dto.LinkToVeterinaryResponse;
import com.MyAnimaLog.Veterinary.application.patients.ports.in.LinkToVeterinaryUseCase;
import com.MyAnimaLog.Veterinary.domain.patients.enums.LinkStatus;
import com.MyAnimaLog.Veterinary.domain.patients.exceptions.InvalidInviteCodeException;
import com.MyAnimaLog.Veterinary.domain.patients.exceptions.LinkCodeNotFoundException;
import com.MyAnimaLog.Veterinary.infrastructure.config.GlobalExceptionHandler;
import com.MyAnimaLog.Veterinary.infrastructure.security.ImportSecurityConfig;
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

@WebMvcTest(controllers = LinkToVeterinaryController.class)
@Import(GlobalExceptionHandler.class)
@ImportSecurityConfig
class LinkToVeterinaryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LinkToVeterinaryUseCase linkToVeterinaryUseCase;

    private UUID veterinaryId;
    private LinkToVeterinaryRequest request;
    private LinkToVeterinaryResponse response;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();
        request = LinkToVeterinaryRequest.builder().code("ABCD2345").build();
        response = LinkToVeterinaryResponse.builder()
                .id(UUID.randomUUID())
                .veterinaryId(veterinaryId)
                .veterinaryName("Clínica El Bosque")
                .logoUrl("https://cdn.test/logo.png")
                .status(LinkStatus.LINKED)
                .linkedAt(LocalDateTime.now())
                .alreadyLinked(false)
                .build();
    }

    @Test
    void link_shouldReturn200_whenLinkIsCreated() throws Exception {
        when(linkToVeterinaryUseCase.link(any(LinkToVeterinaryRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/veterinary/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.veterinaryId").value(veterinaryId.toString()))
                .andExpect(jsonPath("$.veterinaryName").value("Clínica El Bosque"))
                .andExpect(jsonPath("$.status").value("LINKED"))
                .andExpect(jsonPath("$.alreadyLinked").value(false));
    }

    @Test
    void link_shouldReturn200WithAlreadyLinked_whenUserWasAlreadyLinked() throws Exception {
        when(linkToVeterinaryUseCase.link(any(LinkToVeterinaryRequest.class)))
                .thenReturn(LinkToVeterinaryResponse.builder()
                        .id(UUID.randomUUID())
                        .veterinaryId(veterinaryId)
                        .veterinaryName("Clínica El Bosque")
                        .status(LinkStatus.LINKED)
                        .linkedAt(LocalDateTime.now())
                        .alreadyLinked(true)
                        .build());

        // Idempotente: no es 409. Un reintento del frontend no debería verse como un error.
        mockMvc.perform(post("/api/veterinary/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alreadyLinked").value(true));
    }

    @Test
    void link_shouldReturn404_whenCodeDoesNotResolveToAnActiveClinic() throws Exception {
        when(linkToVeterinaryUseCase.link(any(LinkToVeterinaryRequest.class)))
                .thenThrow(new LinkCodeNotFoundException());

        mockMvc.perform(post("/api/veterinary/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LINK_CODE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("No active veterinary matches that link code"));
    }

    @Test
    void link_shouldReturn400_whenCodeIsMissing() throws Exception {
        when(linkToVeterinaryUseCase.link(any(LinkToVeterinaryRequest.class)))
                .thenThrow(new InvalidInviteCodeException("Link code is required"));

        mockMvc.perform(post("/api/veterinary/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INVITE_CODE"));
    }

    @Test
    void link_shouldReturn401_whenNoTokenIsPresent() throws Exception {
        mockMvc.perform(post("/api/veterinary/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}

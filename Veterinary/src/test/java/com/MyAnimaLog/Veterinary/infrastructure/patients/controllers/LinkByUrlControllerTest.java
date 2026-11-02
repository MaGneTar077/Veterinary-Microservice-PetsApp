package com.MyAnimaLog.Veterinary.infrastructure.patients.controllers;

import com.MyAnimaLog.Veterinary.application.patients.dto.LinkByUrlRequest;
import com.MyAnimaLog.Veterinary.application.patients.dto.LinkByUrlResponse;
import com.MyAnimaLog.Veterinary.application.patients.ports.in.LinkByUrlUseCase;
import com.MyAnimaLog.Veterinary.domain.patients.exceptions.InvalidInviteCodeException;
import com.MyAnimaLog.Veterinary.domain.patients.exceptions.UserAlreadyLinkedException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VeterinaryNotActiveException;
import com.MyAnimaLog.Veterinary.infrastructure.config.GlobalExceptionHandler;
import com.MyAnimaLog.Veterinary.infrastructure.security.ImportSecurityConfig;
import com.MyAnimaLog.Veterinary.infrastructure.patients.controllers.LinkByUrlController;
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

@WebMvcTest(controllers = LinkByUrlController.class)
@Import(GlobalExceptionHandler.class)
@ImportSecurityConfig
class LinkByUrlControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LinkByUrlUseCase linkByUrlUseCase;

    private UUID userId;
    private LinkByUrlRequest validRequest;
    private LinkByUrlResponse validResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        validRequest = LinkByUrlRequest.builder()
                .inviteLink("http://localhost:8082/api/veterinary/join?code=VET-DNU4WXDQ")
                .build();

        validResponse = LinkByUrlResponse.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .veterinaryId(UUID.randomUUID())
                .status("LINKED")
                .linkedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void linkByUrl_shouldReturn201_whenRequestIsValid() throws Exception {
        when(linkByUrlUseCase.linkByUrl(any(LinkByUrlRequest.class))).thenReturn(validResponse);

        mockMvc.perform(post("/api/veterinary/link/url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest))
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.status").value("LINKED"))
                .andExpect(jsonPath("$.linkedAt").isNotEmpty());
    }

    @Test
    void linkByUrl_shouldReturn400_whenInviteLinkIsInvalid() throws Exception {
        when(linkByUrlUseCase.linkByUrl(any(LinkByUrlRequest.class)))
                .thenThrow(new InvalidInviteCodeException("Invite link is invalid or does not exist"));

        mockMvc.perform(post("/api/veterinary/link/url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest))
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invite link is invalid or does not exist"));
    }

    @Test
    void linkByUrl_shouldReturn409_whenVeterinaryIsInactive() throws Exception {
        when(linkByUrlUseCase.linkByUrl(any(LinkByUrlRequest.class)))
                .thenThrow(new VeterinaryNotActiveException());

        mockMvc.perform(post("/api/veterinary/link/url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest))
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Veterinary is not active"));
    }

    @Test
    void linkByUrl_shouldReturn409_whenUserAlreadyLinked() throws Exception {
        when(linkByUrlUseCase.linkByUrl(any(LinkByUrlRequest.class)))
                .thenThrow(new UserAlreadyLinkedException());

        mockMvc.perform(post("/api/veterinary/link/url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest))
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("User is already linked to this veterinary"));
    }

    @Test
    void linkByUrl_shouldReturn401_whenNoTokenIsPresent() throws Exception {
        mockMvc.perform(post("/api/veterinary/link/url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest))
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}
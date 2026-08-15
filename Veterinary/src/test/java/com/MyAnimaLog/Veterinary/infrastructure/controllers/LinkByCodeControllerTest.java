package com.MyAnimaLog.Veterinary.infrastructure.controllers;

import com.MyAnimaLog.Veterinary.application.dto.LinkByCodeRequest;
import com.MyAnimaLog.Veterinary.application.dto.LinkByCodeResponse;
import com.MyAnimaLog.Veterinary.application.ports.in.LinkByCodeUseCase;
import com.MyAnimaLog.Veterinary.domain.exceptions.InvalidInviteCodeException;
import com.MyAnimaLog.Veterinary.domain.exceptions.UserAlreadyLinkedException;
import com.MyAnimaLog.Veterinary.domain.exceptions.VeterinaryNotActiveException;
import com.MyAnimaLog.Veterinary.infrastructure.config.GlobalExceptionHandler;
import com.MyAnimaLog.Veterinary.infrastructure.config.SecurityConfig;
import com.MyAnimaLog.Veterinary.infrastructure.controllers.LinkByCodeController;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = LinkByCodeController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityConfig.class
        )
)
@Import(GlobalExceptionHandler.class)
@WithMockUser
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
                .userId(userId)
                .inviteCode("VET-DNU4WXDQ")
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
    void linkByCode_shouldReturn201_whenRequestIsValid() throws Exception {
        when(linkByCodeUseCase.linkByCode(any(LinkByCodeRequest.class))).thenReturn(validResponse);

        mockMvc.perform(post("/api/veterinary/link/code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest))
                        .with(csrf()))
                .andExpect(status().isCreated())
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
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invite code is invalid or does not exist"));
    }

    @Test
    void linkByCode_shouldReturn409_whenVeterinaryIsInactive() throws Exception {
        when(linkByCodeUseCase.linkByCode(any(LinkByCodeRequest.class)))
                .thenThrow(new VeterinaryNotActiveException());

        mockMvc.perform(post("/api/veterinary/link/code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest))
                        .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Veterinary is not active"));
    }

    @Test
    void linkByCode_shouldReturn409_whenUserAlreadyLinked() throws Exception {
        when(linkByCodeUseCase.linkByCode(any(LinkByCodeRequest.class)))
                .thenThrow(new UserAlreadyLinkedException());

        mockMvc.perform(post("/api/veterinary/link/code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest))
                        .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("User is already linked to this veterinary"));
    }
}
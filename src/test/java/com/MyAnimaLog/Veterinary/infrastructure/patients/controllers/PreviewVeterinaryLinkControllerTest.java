package com.MyAnimaLog.Veterinary.infrastructure.patients.controllers;

import com.MyAnimaLog.Veterinary.application.patients.dto.PreviewVeterinaryLinkRequest;
import com.MyAnimaLog.Veterinary.application.patients.dto.PreviewVeterinaryLinkResponse;
import com.MyAnimaLog.Veterinary.application.patients.ports.in.PreviewVeterinaryLinkUseCase;
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

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PreviewVeterinaryLinkController.class)
@Import(GlobalExceptionHandler.class)
@ImportSecurityConfig
class PreviewVeterinaryLinkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PreviewVeterinaryLinkUseCase previewVeterinaryLinkUseCase;

    private UUID veterinaryId;
    private PreviewVeterinaryLinkRequest request;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();
        request = PreviewVeterinaryLinkRequest.builder().code("ABCD2345").build();
    }

    @Test
    void preview_shouldReturn200_withTheClinicData() throws Exception {
        when(previewVeterinaryLinkUseCase.preview(any(PreviewVeterinaryLinkRequest.class)))
                .thenReturn(PreviewVeterinaryLinkResponse.builder()
                        .veterinaryId(veterinaryId)
                        .name("Clínica El Bosque")
                        .logoUrl("https://cdn.test/logo.png")
                        .city("Cartagena")
                        .department("Bolívar")
                        .address("Calle 1 #2-3")
                        .alreadyLinked(false)
                        .build());

        mockMvc.perform(post("/api/veterinary/links/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.veterinaryId").value(veterinaryId.toString()))
                .andExpect(jsonPath("$.name").value("Clínica El Bosque"))
                .andExpect(jsonPath("$.city").value("Cartagena"))
                .andExpect(jsonPath("$.alreadyLinked").value(false))
                // Nada interno: cualquiera con un código llega acá.
                .andExpect(jsonPath("$.nit").doesNotExist())
                .andExpect(jsonPath("$.legalName").doesNotExist())
                .andExpect(jsonPath("$.status").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.phone").doesNotExist());
    }

    @Test
    void preview_shouldReturn404_whenCodeDoesNotResolveToAnActiveClinic() throws Exception {
        when(previewVeterinaryLinkUseCase.preview(any(PreviewVeterinaryLinkRequest.class)))
                .thenThrow(new LinkCodeNotFoundException());

        mockMvc.perform(post("/api/veterinary/links/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LINK_CODE_NOT_FOUND"));
    }

    @Test
    void preview_shouldReturn401_whenNoTokenIsPresent() throws Exception {
        mockMvc.perform(post("/api/veterinary/links/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}

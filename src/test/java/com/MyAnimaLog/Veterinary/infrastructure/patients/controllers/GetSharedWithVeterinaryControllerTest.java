package com.MyAnimaLog.Veterinary.infrastructure.patients.controllers;

import com.MyAnimaLog.Veterinary.application.patients.dto.GetSharedWithVeterinaryResponse;
import com.MyAnimaLog.Veterinary.application.patients.ports.in.GetSharedWithVeterinaryUseCase;
import com.MyAnimaLog.Veterinary.infrastructure.config.GlobalExceptionHandler;
import com.MyAnimaLog.Veterinary.infrastructure.security.ImportSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GetSharedWithVeterinaryController.class)
@Import(GlobalExceptionHandler.class)
@ImportSecurityConfig
class GetSharedWithVeterinaryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetSharedWithVeterinaryUseCase getSharedWithVeterinaryUseCase;

    @Value("${app.security.internal-api-key}")
    private String internalApiKey;

    @Test
    void get_shouldReturn401_whenInternalApiKeyIsMissing() throws Exception {
        mockMvc.perform(get("/internal/veterinaries/{id}/patients/{petId}/shared",
                        UUID.randomUUID(), UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void get_shouldReturn200WithSharedTrue() throws Exception {
        when(getSharedWithVeterinaryUseCase.get(any(), any()))
                .thenReturn(GetSharedWithVeterinaryResponse.builder().shared(true).build());

        mockMvc.perform(get("/internal/veterinaries/{id}/patients/{petId}/shared",
                        UUID.randomUUID(), UUID.randomUUID())
                        .header("X-Internal-Api-Key", internalApiKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shared").value(true));
    }

    @Test
    void get_shouldReturn200WithSharedFalse_neverA404() throws Exception {
        when(getSharedWithVeterinaryUseCase.get(any(), any()))
                .thenReturn(GetSharedWithVeterinaryResponse.builder().shared(false).build());

        mockMvc.perform(get("/internal/veterinaries/{id}/patients/{petId}/shared",
                        UUID.randomUUID(), UUID.randomUUID())
                        .header("X-Internal-Api-Key", internalApiKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shared").value(false));
    }
}

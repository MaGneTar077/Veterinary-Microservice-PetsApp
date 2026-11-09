package com.MyAnimaLog.Veterinary.infrastructure.patients.controllers;

import com.MyAnimaLog.Veterinary.application.patients.dto.ListVeterinaryPatientsResponse;
import com.MyAnimaLog.Veterinary.application.patients.ports.in.ListVeterinaryPatientsUseCase;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.ExternalServiceUnavailableException;
import com.MyAnimaLog.Veterinary.infrastructure.config.GlobalExceptionHandler;
import com.MyAnimaLog.Veterinary.infrastructure.security.ImportSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ListVeterinaryPatientsController.class)
@Import(GlobalExceptionHandler.class)
@ImportSecurityConfig
class ListVeterinaryPatientsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListVeterinaryPatientsUseCase listVeterinaryPatientsUseCase;

    @Test
    void list_shouldReturn200() throws Exception {
        when(listVeterinaryPatientsUseCase.list(any(), any(), any())).thenReturn(
                ListVeterinaryPatientsResponse.builder().items(List.of()).page(0).size(20).total(0).build());

        mockMvc.perform(get("/api/veterinary/{id}/patients", UUID.randomUUID()).with(jwt()))
                .andExpect(status().isOk());
    }

    @Test
    void list_shouldReturn503_whenPetsIsDown() throws Exception {
        when(listVeterinaryPatientsUseCase.list(any(), any(), any()))
                .thenThrow(new ExternalServiceUnavailableException("Pets service lookup failed"));

        mockMvc.perform(get("/api/veterinary/{id}/patients", UUID.randomUUID()).with(jwt()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("EXTERNAL_SERVICE_UNAVAILABLE"));
    }

    @Test
    void list_shouldReturn401_whenNoTokenIsPresent() throws Exception {
        mockMvc.perform(get("/api/veterinary/{id}/patients", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }
}

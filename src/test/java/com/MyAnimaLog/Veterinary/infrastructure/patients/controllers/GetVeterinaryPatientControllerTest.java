package com.MyAnimaLog.Veterinary.infrastructure.patients.controllers;

import com.MyAnimaLog.Veterinary.application.patients.dto.GetVeterinaryPatientResponse;
import com.MyAnimaLog.Veterinary.application.patients.ports.in.GetVeterinaryPatientUseCase;
import com.MyAnimaLog.Veterinary.domain.patients.exceptions.PatientNotFoundException;
import com.MyAnimaLog.Veterinary.infrastructure.config.GlobalExceptionHandler;
import com.MyAnimaLog.Veterinary.infrastructure.security.ImportSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = GetVeterinaryPatientController.class)
@Import(GlobalExceptionHandler.class)
@ImportSecurityConfig
class GetVeterinaryPatientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetVeterinaryPatientUseCase getVeterinaryPatientUseCase;

    @Test
    void get_shouldReturn200() throws Exception {
        when(getVeterinaryPatientUseCase.get(any(), any())).thenReturn(
                GetVeterinaryPatientResponse.builder().name("Max").build());

        mockMvc.perform(get("/api/veterinary/{id}/patients/{petId}", UUID.randomUUID(), UUID.randomUUID())
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Max"));
    }

    @Test
    void get_shouldReturn404_whenRevokedOrNeverShared() throws Exception {
        when(getVeterinaryPatientUseCase.get(any(), any())).thenThrow(new PatientNotFoundException());

        mockMvc.perform(get("/api/veterinary/{id}/patients/{petId}", UUID.randomUUID(), UUID.randomUUID())
                        .with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PATIENT_NOT_FOUND"));
    }

    @Test
    void get_shouldReturn401_whenNoTokenIsPresent() throws Exception {
        mockMvc.perform(get("/api/veterinary/{id}/patients/{petId}", UUID.randomUUID(), UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }
}

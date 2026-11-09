package com.MyAnimaLog.Veterinary.infrastructure.patients.controllers;

import com.MyAnimaLog.Veterinary.application.patients.dto.GetVeterinarySummaryResponse;
import com.MyAnimaLog.Veterinary.application.patients.ports.in.GetVeterinarySummaryUseCase;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VeterinaryNotFoundException;
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

@WebMvcTest(controllers = GetVeterinarySummaryController.class)
@Import(GlobalExceptionHandler.class)
@ImportSecurityConfig
class GetVeterinarySummaryControllerTest {

    private static final String HEADER = "X-Internal-Api-Key";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetVeterinarySummaryUseCase getVeterinarySummaryUseCase;

    @Value("${app.security.internal-api-key}")
    private String internalApiKey;

    @Test
    void getSummary_shouldReturn401_whenInternalApiKeyIsMissing() throws Exception {
        mockMvc.perform(get("/internal/veterinaries/{id}/summary", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getSummary_shouldReturn200_whenClinicExists() throws Exception {
        UUID veterinaryId = UUID.randomUUID();
        when(getVeterinarySummaryUseCase.getSummary(any())).thenReturn(GetVeterinarySummaryResponse.builder()
                .id(veterinaryId).name("Clínica El Bosque").timezone("America/Bogota")
                .status(VeterinaryStatus.ACTIVE).build());

        mockMvc.perform(get("/internal/veterinaries/{id}/summary", veterinaryId).header(HEADER, internalApiKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Clínica El Bosque"))
                .andExpect(jsonPath("$.timezone").value("America/Bogota"));
    }

    @Test
    void getSummary_shouldReturn404_whenClinicDoesNotExist() throws Exception {
        when(getVeterinarySummaryUseCase.getSummary(any())).thenThrow(new VeterinaryNotFoundException());

        mockMvc.perform(get("/internal/veterinaries/{id}/summary", UUID.randomUUID())
                        .header(HEADER, internalApiKey))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("VETERINARY_NOT_FOUND"));
    }
}

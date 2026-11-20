package com.MyAnimaLog.Veterinary.infrastructure.catalog.controllers;

import com.MyAnimaLog.Veterinary.application.catalog.ports.in.GetInternalClinicServiceUseCase;
import com.MyAnimaLog.Veterinary.domain.catalog.exceptions.ClinicServiceNotFoundException;
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

@WebMvcTest(controllers = GetInternalClinicServiceController.class)
@Import(GlobalExceptionHandler.class)
@ImportSecurityConfig
class GetInternalClinicServiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetInternalClinicServiceUseCase getInternalClinicServiceUseCase;

    @Value("${app.security.internal-api-key}")
    private String internalApiKey;

    @Test
    void get_shouldReturn401_whenInternalApiKeyIsMissing() throws Exception {
        mockMvc.perform(get("/internal/veterinaries/" + UUID.randomUUID() + "/services/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void get_shouldReturn404_whenServiceBelongsToAnotherClinic() throws Exception {
        when(getInternalClinicServiceUseCase.get(any(), any())).thenThrow(new ClinicServiceNotFoundException());

        mockMvc.perform(get("/internal/veterinaries/" + UUID.randomUUID() + "/services/" + UUID.randomUUID())
                        .header("X-Internal-Api-Key", internalApiKey))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CLINIC_SERVICE_NOT_FOUND"));
    }
}

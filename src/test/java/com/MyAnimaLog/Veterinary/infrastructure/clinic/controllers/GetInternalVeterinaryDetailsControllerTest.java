package com.MyAnimaLog.Veterinary.infrastructure.clinic.controllers;

import com.MyAnimaLog.Veterinary.application.clinic.dto.GetInternalVeterinaryDetailsResponse;
import com.MyAnimaLog.Veterinary.application.clinic.ports.in.GetInternalVeterinaryDetailsUseCase;
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

@WebMvcTest(controllers = GetInternalVeterinaryDetailsController.class)
@Import(GlobalExceptionHandler.class)
@ImportSecurityConfig
class GetInternalVeterinaryDetailsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetInternalVeterinaryDetailsUseCase getInternalVeterinaryDetailsUseCase;

    @Value("${app.security.internal-api-key}")
    private String internalApiKey;

    @Test
    void get_shouldReturn401_whenInternalApiKeyIsMissing() throws Exception {
        mockMvc.perform(get("/internal/veterinaries/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void get_shouldReturnTheExactJsonShape() throws Exception {
        UUID veterinaryId = UUID.randomUUID();
        when(getInternalVeterinaryDetailsUseCase.get(any())).thenReturn(GetInternalVeterinaryDetailsResponse.builder()
                .id(veterinaryId).name("Clínica El Bosque").status(VeterinaryStatus.ACTIVE)
                .allowOnlineBooking(true).bookingRequiresConfirmation(true).cancellationMinHours(4)
                .timezone("America/Bogota").address("Calle 1 #2-3").phone("3001234567").build());

        mockMvc.perform(get("/internal/veterinaries/{id}", veterinaryId).header("X-Internal-Api-Key", internalApiKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(veterinaryId.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.allowOnlineBooking").value(true))
                .andExpect(jsonPath("$.cancellationMinHours").value(4))
                .andExpect(jsonPath("$.address").value("Calle 1 #2-3"))
                .andExpect(jsonPath("$.phone").value("3001234567"))
                .andExpect(jsonPath("$.nit").doesNotExist())
                .andExpect(jsonPath("$.legalName").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist());
    }

    @Test
    void get_shouldReturn404_whenTheClinicDoesNotExist() throws Exception {
        when(getInternalVeterinaryDetailsUseCase.get(any())).thenThrow(new VeterinaryNotFoundException());

        mockMvc.perform(get("/internal/veterinaries/{id}", UUID.randomUUID())
                        .header("X-Internal-Api-Key", internalApiKey))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("VETERINARY_NOT_FOUND"));
    }
}

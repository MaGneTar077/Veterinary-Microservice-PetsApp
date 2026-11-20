package com.MyAnimaLog.Veterinary.infrastructure.catalog.controllers;

import com.MyAnimaLog.Veterinary.application.catalog.dto.InternalClinicServiceResponse;
import com.MyAnimaLog.Veterinary.application.catalog.ports.in.GetInternalClinicServicesUseCase;
import com.MyAnimaLog.Veterinary.domain.catalog.exceptions.InvalidClinicServiceException;
import com.MyAnimaLog.Veterinary.infrastructure.config.GlobalExceptionHandler;
import com.MyAnimaLog.Veterinary.infrastructure.security.ImportSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GetInternalClinicServicesController.class)
@Import(GlobalExceptionHandler.class)
@ImportSecurityConfig
class GetInternalClinicServicesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetInternalClinicServicesUseCase getInternalClinicServicesUseCase;

    @Value("${app.security.internal-api-key}")
    private String internalApiKey;

    @Test
    void list_shouldReturn401_whenInternalApiKeyIsMissing() throws Exception {
        mockMvc.perform(get("/internal/veterinaries/" + UUID.randomUUID() + "/services"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void list_shouldReturn200_withEmployeeIdsAndPriceAsNumber() throws Exception {
        UUID veterinaryId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        when(getInternalClinicServicesUseCase.list(any(), anyBoolean(), any())).thenReturn(List.of(
                InternalClinicServiceResponse.builder()
                        .id(UUID.randomUUID())
                        .name("Consulta general")
                        .category("CONSULTATION")
                        .durationMinutes(30)
                        .price(BigDecimal.valueOf(40000))
                        .active(true)
                        .bookableOnline(true)
                        .requiresLicense(false)
                        .employeeIds(List.of(employeeId))
                        .build()));

        mockMvc.perform(get("/internal/veterinaries/" + veterinaryId + "/services")
                        .header("X-Internal-Api-Key", internalApiKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].employeeIds[0]").value(employeeId.toString()))
                .andExpect(jsonPath("$[0].price").value(40000));
    }

    @Test
    void list_shouldReturn400_whenBookableOnlineIsNotTrueOrFalse() throws Exception {
        when(getInternalClinicServicesUseCase.list(any(), anyBoolean(), any()))
                .thenThrow(new InvalidClinicServiceException("bookableOnline must be 'true' or 'false', got: yes"));

        mockMvc.perform(get("/internal/veterinaries/" + UUID.randomUUID() + "/services?bookableOnline=yes")
                        .header("X-Internal-Api-Key", internalApiKey))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}

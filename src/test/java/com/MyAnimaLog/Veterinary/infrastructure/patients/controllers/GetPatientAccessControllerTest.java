package com.MyAnimaLog.Veterinary.infrastructure.patients.controllers;

import com.MyAnimaLog.Veterinary.application.patients.dto.GetPatientAccessResponse;
import com.MyAnimaLog.Veterinary.application.patients.ports.in.GetPatientAccessUseCase;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.security.Permission;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.infrastructure.config.GlobalExceptionHandler;
import com.MyAnimaLog.Veterinary.infrastructure.security.ImportSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GetPatientAccessController.class)
@Import(GlobalExceptionHandler.class)
@ImportSecurityConfig
class GetPatientAccessControllerTest {

    private static final String HEADER = "X-Internal-Api-Key";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetPatientAccessUseCase getPatientAccessUseCase;

    @Value("${app.security.internal-api-key}")
    private String internalApiKey;

    private String url(UUID veterinaryId, UUID petId, UUID userId) {
        return "/internal/veterinaries/" + veterinaryId + "/patients/" + petId + "/access?userId=" + userId;
    }

    @Test
    void getAccess_shouldReturn401_whenInternalApiKeyIsMissing() throws Exception {
        mockMvc.perform(get(url(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAccess_shouldReturn200WithHasAccessTrue() throws Exception {
        UUID veterinaryId = UUID.randomUUID();
        UUID petId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        when(getPatientAccessUseCase.getAccess(any(), any(), any())).thenReturn(GetPatientAccessResponse.builder()
                .hasAccess(true)
                .veterinaryStatus(VeterinaryStatus.ACTIVE)
                .employeeId(employeeId)
                .role(EmployeeRole.VETERINARIAN)
                .licensed(true)
                .permissions(List.of(Permission.CLINICAL_READ, Permission.CLINICAL_WRITE))
                .build());

        mockMvc.perform(get(url(veterinaryId, petId, userId)).header(HEADER, internalApiKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasAccess").value(true))
                .andExpect(jsonPath("$.employeeId").value(employeeId.toString()))
                .andExpect(jsonPath("$.role").value("VETERINARIAN"))
                .andExpect(jsonPath("$.permissions[0]").value("CLINICAL_READ"));
    }

    @Test
    void getAccess_shouldReturn200WithHasAccessFalse_neverNotFound() throws Exception {
        when(getPatientAccessUseCase.getAccess(any(), any(), any())).thenReturn(GetPatientAccessResponse.builder()
                .hasAccess(false)
                .licensed(false)
                .permissions(List.of())
                .build());

        mockMvc.perform(get(url(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()))
                        .header(HEADER, internalApiKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasAccess").value(false));
    }
}

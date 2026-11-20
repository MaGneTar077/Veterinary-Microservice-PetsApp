package com.MyAnimaLog.Veterinary.infrastructure.staff.controllers;

import com.MyAnimaLog.Veterinary.application.staff.dto.InternalVeterinaryMemberResponse;
import com.MyAnimaLog.Veterinary.application.staff.ports.in.GetInternalVeterinaryMembersUseCase;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VeterinaryNotFoundException;
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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GetInternalVeterinaryMembersController.class)
@Import(GlobalExceptionHandler.class)
@ImportSecurityConfig
class GetInternalVeterinaryMembersControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetInternalVeterinaryMembersUseCase getInternalVeterinaryMembersUseCase;

    @Value("${app.security.internal-api-key}")
    private String internalApiKey;

    @Test
    void list_shouldReturn401_whenInternalApiKeyIsMissing() throws Exception {
        mockMvc.perform(get("/internal/veterinaries/{id}/members", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void list_shouldReturnTheJsonShape_withoutEmailOrPhone() throws Exception {
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(getInternalVeterinaryMembersUseCase.list(any(), anyBoolean())).thenReturn(List.of(
                InternalVeterinaryMemberResponse.builder()
                        .employeeId(employeeId).userId(userId).name("Ana Pérez")
                        .role(EmployeeRole.VETERINARIAN).active(true).licensed(true)
                        .permissions(List.of("APPOINTMENT_MANAGE"))
                        .build()));

        mockMvc.perform(get("/internal/veterinaries/{id}/members", UUID.randomUUID())
                        .header("X-Internal-Api-Key", internalApiKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].employeeId").value(employeeId.toString()))
                .andExpect(jsonPath("$[0].userId").value(userId.toString()))
                .andExpect(jsonPath("$[0].name").value("Ana Pérez"))
                .andExpect(jsonPath("$[0].licensed").value(true))
                .andExpect(jsonPath("$[0].permissions[0]").value("APPOINTMENT_MANAGE"))
                .andExpect(jsonPath("$[0].email").doesNotExist())
                .andExpect(jsonPath("$[0].phone").doesNotExist());
    }

    @Test
    void list_shouldReturn404_whenTheClinicDoesNotExist() throws Exception {
        when(getInternalVeterinaryMembersUseCase.list(any(), anyBoolean()))
                .thenThrow(new VeterinaryNotFoundException());

        mockMvc.perform(get("/internal/veterinaries/{id}/members", UUID.randomUUID())
                        .header("X-Internal-Api-Key", internalApiKey))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("VETERINARY_NOT_FOUND"));
    }
}

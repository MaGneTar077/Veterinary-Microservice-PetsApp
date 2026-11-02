package com.MyAnimaLog.Veterinary.infrastructure.staff.controllers;

import com.MyAnimaLog.Veterinary.application.staff.dto.UpdateEmployeeRoleRequest;
import com.MyAnimaLog.Veterinary.application.staff.dto.UpdateEmployeeRoleResponse;
import com.MyAnimaLog.Veterinary.application.staff.ports.in.UpdateEmployeeRoleUseCase;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.EmployeeNotFoundException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.OnlyOwnerCanManageAdminsException;
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
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UpdateEmployeeRoleNestedController.class)
@Import(GlobalExceptionHandler.class)
@ImportSecurityConfig
class UpdateEmployeeRoleNestedControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UpdateEmployeeRoleUseCase updateEmployeeRoleUseCase;

    private UUID veterinaryId;
    private UUID employeeId;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();
        employeeId = UUID.randomUUID();
    }

    private ResultActions perform(EmployeeRole role) throws Exception {
        return mockMvc.perform(patch("/api/veterinary/{veterinaryId}/employees/{employeeId}/role",
                veterinaryId, employeeId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        UpdateEmployeeRoleRequest.builder().role(role).build()))
                .with(csrf())
                .with(jwt()));
    }

    @Test
    void updateRole_shouldReturn200AndPassTheClinicFromThePath() throws Exception {
        when(updateEmployeeRoleUseCase.updateRole(any(), any(UUID.class), any(UpdateEmployeeRoleRequest.class)))
                .thenReturn(UpdateEmployeeRoleResponse.builder()
                        .id(employeeId)
                        .veterinaryId(veterinaryId)
                        .userId(UUID.randomUUID())
                        .role(EmployeeRole.RECEPTIONIST)
                        .build());

        perform(EmployeeRole.RECEPTIONIST)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("RECEPTIONIST"));

        verify(updateEmployeeRoleUseCase).updateRole(eq(veterinaryId), eq(employeeId),
                any(UpdateEmployeeRoleRequest.class));
    }

    @Test
    void updateRole_shouldReturn403_whenAnAdminPromotesSomeoneToAdmin() throws Exception {
        when(updateEmployeeRoleUseCase.updateRole(any(), any(UUID.class), any(UpdateEmployeeRoleRequest.class)))
                .thenThrow(new OnlyOwnerCanManageAdminsException());

        perform(EmployeeRole.ADMIN)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ONLY_OWNER_CAN_MANAGE_ADMINS"));
    }

    @Test
    void updateRole_shouldReturn404_whenTheEmployeeIsFromAnotherClinic() throws Exception {
        when(updateEmployeeRoleUseCase.updateRole(any(), any(UUID.class), any(UpdateEmployeeRoleRequest.class)))
                .thenThrow(new EmployeeNotFoundException());

        perform(EmployeeRole.ASSISTANT)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EMPLOYEE_NOT_FOUND"));
    }

    @Test
    void updateRole_shouldReturn401_whenNoTokenIsPresent() throws Exception {
        mockMvc.perform(patch("/api/veterinary/{veterinaryId}/employees/{employeeId}/role",
                        veterinaryId, employeeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                UpdateEmployeeRoleRequest.builder().role(EmployeeRole.ASSISTANT).build()))
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}

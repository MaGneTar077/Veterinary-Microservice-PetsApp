package com.MyAnimaLog.Veterinary.infrastructure.staff.controllers;

import com.MyAnimaLog.Veterinary.application.staff.dto.UpdateEmployeeRoleRequest;
import com.MyAnimaLog.Veterinary.application.staff.dto.UpdateEmployeeRoleResponse;
import com.MyAnimaLog.Veterinary.application.staff.ports.in.UpdateEmployeeRoleUseCase;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.domain.security.Permission;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.InsufficientPermissionException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.TenantMismatchException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.CannotModifySelfException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.EmployeeNotFoundException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.InvalidEmployeeRoleException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.CannotModifyOwnerException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.CannotModifyPeerAdminException;
import com.MyAnimaLog.Veterinary.infrastructure.config.GlobalExceptionHandler;
import com.MyAnimaLog.Veterinary.infrastructure.security.ImportSecurityConfig;
import com.MyAnimaLog.Veterinary.infrastructure.staff.controllers.UpdateEmployeeRoleController;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UpdateEmployeeRoleController.class)
@Import(GlobalExceptionHandler.class)
@ImportSecurityConfig
class UpdateEmployeeRoleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UpdateEmployeeRoleUseCase updateEmployeeRoleUseCase;

    private UUID employeeId;
    private UpdateEmployeeRoleResponse validResponse;

    @BeforeEach
    void setUp() {
        employeeId = UUID.randomUUID();

        validResponse = UpdateEmployeeRoleResponse.builder()
                .id(employeeId)
                .veterinaryId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .role(EmployeeRole.ADMIN)
                .build();
    }

    @Test
    void updateRole_shouldReturn200_whenRequestIsValid() throws Exception {
        when(updateEmployeeRoleUseCase.updateRole(any(UUID.class), any(UpdateEmployeeRoleRequest.class)))
                .thenReturn(validResponse);

        mockMvc.perform(patch("/api/veterinary/employees/{employeeId}/role", employeeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                UpdateEmployeeRoleRequest.builder().role(EmployeeRole.ADMIN).build()
                        ))
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(employeeId.toString()))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void updateRole_shouldReturn400_whenRoleIsInvalid() throws Exception {
        when(updateEmployeeRoleUseCase.updateRole(any(UUID.class), any(UpdateEmployeeRoleRequest.class)))
                .thenThrow(new InvalidEmployeeRoleException());

        mockMvc.perform(patch("/api/veterinary/employees/{employeeId}/role", employeeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                UpdateEmployeeRoleRequest.builder().build()
                        ))
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Employee role is invalid"));
    }

    @Test
    void updateRole_shouldReturn404_whenEmployeeNotFound() throws Exception {
        when(updateEmployeeRoleUseCase.updateRole(any(UUID.class), any(UpdateEmployeeRoleRequest.class)))
                .thenThrow(new EmployeeNotFoundException());

        mockMvc.perform(patch("/api/veterinary/employees/{employeeId}/role", employeeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                UpdateEmployeeRoleRequest.builder().role(EmployeeRole.ADMIN).build()
                        ))
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Employee not found"));
    }

    @Test
    void updateRole_shouldReturn401_whenNoTokenIsPresent() throws Exception {
        mockMvc.perform(patch("/api/veterinary/employees/{employeeId}/role", employeeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                UpdateEmployeeRoleRequest.builder().role(EmployeeRole.ADMIN).build()
                        ))
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateRole_shouldReturn403_whenCallerLacksStaffManage() throws Exception {
        when(updateEmployeeRoleUseCase.updateRole(any(UUID.class), any(UpdateEmployeeRoleRequest.class)))
                .thenThrow(new InsufficientPermissionException(Permission.STAFF_MANAGE));

        mockMvc.perform(patch("/api/veterinary/employees/{employeeId}/role", employeeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                UpdateEmployeeRoleRequest.builder().role(EmployeeRole.ADMIN).build()
                        ))
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateRole_shouldReturn403_whenCallerBelongsToAnotherVeterinary() throws Exception {
        when(updateEmployeeRoleUseCase.updateRole(any(UUID.class), any(UpdateEmployeeRoleRequest.class)))
                .thenThrow(new TenantMismatchException());

        mockMvc.perform(patch("/api/veterinary/employees/{employeeId}/role", employeeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                UpdateEmployeeRoleRequest.builder().role(EmployeeRole.ADMIN).build()
                        ))
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateRole_shouldReturn403_whenActorTargetsOwnRole() throws Exception {
        when(updateEmployeeRoleUseCase.updateRole(any(UUID.class), any(UpdateEmployeeRoleRequest.class)))
                .thenThrow(new CannotModifySelfException());

        mockMvc.perform(patch("/api/veterinary/employees/{employeeId}/role", employeeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                UpdateEmployeeRoleRequest.builder().role(EmployeeRole.ASSISTANT).build()
                        ))
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateRole_shouldReturn403_whenTargetIsOwner() throws Exception {
        when(updateEmployeeRoleUseCase.updateRole(any(UUID.class), any(UpdateEmployeeRoleRequest.class)))
                .thenThrow(new CannotModifyOwnerException());

        mockMvc.perform(patch("/api/veterinary/employees/{employeeId}/role", employeeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                UpdateEmployeeRoleRequest.builder().role(EmployeeRole.VETERINARIAN).build()
                        ))
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateRole_shouldReturn403_whenActingAdminTargetsAnotherAdmin() throws Exception {
        when(updateEmployeeRoleUseCase.updateRole(any(UUID.class), any(UpdateEmployeeRoleRequest.class)))
                .thenThrow(new CannotModifyPeerAdminException());

        mockMvc.perform(patch("/api/veterinary/employees/{employeeId}/role", employeeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                UpdateEmployeeRoleRequest.builder().role(EmployeeRole.VETERINARIAN).build()
                        ))
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isForbidden());
    }
}
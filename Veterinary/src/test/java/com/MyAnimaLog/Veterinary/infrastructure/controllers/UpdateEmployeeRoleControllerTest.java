package com.MyAnimaLog.Veterinary.infrastructure.controllers;

import com.MyAnimaLog.Veterinary.application.dto.UpdateEmployeeRoleRequest;
import com.MyAnimaLog.Veterinary.application.dto.UpdateEmployeeRoleResponse;
import com.MyAnimaLog.Veterinary.application.ports.in.UpdateEmployeeRoleUseCase;
import com.MyAnimaLog.Veterinary.domain.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.domain.exceptions.EmployeeNotFoundException;
import com.MyAnimaLog.Veterinary.domain.exceptions.InvalidEmployeeRoleException;
import com.MyAnimaLog.Veterinary.infrastructure.config.GlobalExceptionHandler;
import com.MyAnimaLog.Veterinary.infrastructure.config.SecurityConfig;
import com.MyAnimaLog.Veterinary.infrastructure.controllers.UpdateEmployeeRoleController;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = UpdateEmployeeRoleController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityConfig.class
        )
)
@Import(GlobalExceptionHandler.class)
@WithMockUser
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
                        .with(csrf()))
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
                        .with(csrf()))
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
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Employee not found"));
    }
}
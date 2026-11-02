package com.MyAnimaLog.Veterinary.infrastructure.staff.controllers;

import com.MyAnimaLog.Veterinary.application.staff.dto.ActivateEmployeeResponse;
import com.MyAnimaLog.Veterinary.application.staff.ports.in.ActivateEmployeeUseCase;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.domain.security.Permission;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.InsufficientPermissionException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.EmployeeNotFoundException;
import com.MyAnimaLog.Veterinary.infrastructure.config.GlobalExceptionHandler;
import com.MyAnimaLog.Veterinary.infrastructure.security.ImportSecurityConfig;
import com.MyAnimaLog.Veterinary.infrastructure.staff.controllers.ActivateEmployeeController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ActivateEmployeeController.class)
@Import(GlobalExceptionHandler.class)
@ImportSecurityConfig
class ActivateEmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ActivateEmployeeUseCase activateEmployeeUseCase;

    private UUID employeeId;
    private ActivateEmployeeResponse validResponse;

    @BeforeEach
    void setUp() {
        employeeId = UUID.randomUUID();

        validResponse = ActivateEmployeeResponse.builder()
                .id(employeeId)
                .veterinaryId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .role(EmployeeRole.VETERINARIAN)
                .active(true)
                .build();
    }

    @Test
    void activate_shouldReturn200_whenEmployeeExists() throws Exception {
        when(activateEmployeeUseCase.activate(any(UUID.class))).thenReturn(validResponse);

        mockMvc.perform(patch("/api/veterinary/employees/{employeeId}/activate", employeeId)
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(employeeId.toString()))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.role").value("VETERINARIAN"));
    }

    @Test
    void activate_shouldReturn404_whenEmployeeNotFound() throws Exception {
        when(activateEmployeeUseCase.activate(any(UUID.class)))
                .thenThrow(new EmployeeNotFoundException());

        mockMvc.perform(patch("/api/veterinary/employees/{employeeId}/activate", employeeId)
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Employee not found"));
    }

    @Test
    void activate_shouldReturn401_whenNoTokenIsPresent() throws Exception {
        mockMvc.perform(patch("/api/veterinary/employees/{employeeId}/activate", employeeId)
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void activate_shouldReturn403_whenCallerLacksStaffManage() throws Exception {
        when(activateEmployeeUseCase.activate(any(UUID.class)))
                .thenThrow(new InsufficientPermissionException(Permission.STAFF_MANAGE));

        mockMvc.perform(patch("/api/veterinary/employees/{employeeId}/activate", employeeId)
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isForbidden());
    }
}
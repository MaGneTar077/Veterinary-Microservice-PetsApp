package com.MyAnimaLog.Veterinary.infrastructure.controllers;

import com.MyAnimaLog.Veterinary.application.dto.ActivateEmployeeResponse;
import com.MyAnimaLog.Veterinary.application.ports.in.ActivateEmployeeUseCase;
import com.MyAnimaLog.Veterinary.domain.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.domain.exceptions.EmployeeNotFoundException;
import com.MyAnimaLog.Veterinary.infrastructure.config.GlobalExceptionHandler;
import com.MyAnimaLog.Veterinary.infrastructure.config.SecurityConfig;
import com.MyAnimaLog.Veterinary.infrastructure.controllers.ActivateEmployeeController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
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
        controllers = ActivateEmployeeController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityConfig.class
        )
)
@Import(GlobalExceptionHandler.class)
@WithMockUser
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
                        .with(csrf()))
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
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Employee not found"));
    }
}
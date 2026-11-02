package com.MyAnimaLog.Veterinary.infrastructure.staff.controllers;

import com.MyAnimaLog.Veterinary.application.staff.dto.DeActivateEmployeeResponse;
import com.MyAnimaLog.Veterinary.application.staff.ports.in.DeActivateEmployeeUseCase;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.CannotModifyOwnerException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.CannotModifyPeerAdminException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.CannotModifySelfException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.EmployeeNotFoundException;
import com.MyAnimaLog.Veterinary.infrastructure.config.GlobalExceptionHandler;
import com.MyAnimaLog.Veterinary.infrastructure.security.ImportSecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RemoveEmployeeController.class)
@Import(GlobalExceptionHandler.class)
@ImportSecurityConfig
class RemoveEmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DeActivateEmployeeUseCase deActivateEmployeeUseCase;

    private UUID veterinaryId;
    private UUID employeeId;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();
        employeeId = UUID.randomUUID();
    }

    private ResultActions perform() throws Exception {
        return mockMvc.perform(delete("/api/veterinary/{veterinaryId}/employees/{employeeId}",
                veterinaryId, employeeId)
                .with(csrf())
                .with(jwt()));
    }

    @Test
    void remove_shouldReturn200AndTheEmployeeDeactivated_notDeleted() throws Exception {
        when(deActivateEmployeeUseCase.deActivate(any(), any(UUID.class)))
                .thenReturn(DeActivateEmployeeResponse.builder()
                        .id(employeeId)
                        .veterinaryId(veterinaryId)
                        .userId(UUID.randomUUID())
                        .role(EmployeeRole.ASSISTANT)
                        .active(false)
                        .build());

        perform()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(employeeId.toString()))
                .andExpect(jsonPath("$.active").value(false));

        verify(deActivateEmployeeUseCase).deActivate(veterinaryId, employeeId);
    }

    @Test
    void remove_shouldReturn404_whenTheEmployeeIsFromAnotherClinic() throws Exception {
        when(deActivateEmployeeUseCase.deActivate(any(), any(UUID.class)))
                .thenThrow(new EmployeeNotFoundException());

        perform()
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EMPLOYEE_NOT_FOUND"));
    }

    @Test
    void remove_shouldReturn403_whenTargetingTheOwner() throws Exception {
        when(deActivateEmployeeUseCase.deActivate(any(), any(UUID.class)))
                .thenThrow(new CannotModifyOwnerException());

        perform()
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CANNOT_MODIFY_OWNER"));
    }

    @Test
    void remove_shouldReturn403_whenTargetingYourself() throws Exception {
        when(deActivateEmployeeUseCase.deActivate(any(), any(UUID.class)))
                .thenThrow(new CannotModifySelfException());

        perform()
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CANNOT_MODIFY_SELF"));
    }

    @Test
    void remove_shouldReturn403_whenAnAdminTargetsAnotherAdmin() throws Exception {
        when(deActivateEmployeeUseCase.deActivate(any(), any(UUID.class)))
                .thenThrow(new CannotModifyPeerAdminException());

        perform()
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CANNOT_MODIFY_PEER_ADMIN"));
    }

    @Test
    void remove_shouldReturn401_whenNoTokenIsPresent() throws Exception {
        mockMvc.perform(delete("/api/veterinary/{veterinaryId}/employees/{employeeId}",
                        veterinaryId, employeeId).with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}

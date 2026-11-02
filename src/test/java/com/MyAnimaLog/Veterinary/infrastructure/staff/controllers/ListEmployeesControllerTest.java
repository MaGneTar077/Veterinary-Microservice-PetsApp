package com.MyAnimaLog.Veterinary.infrastructure.staff.controllers;

import com.MyAnimaLog.Veterinary.application.staff.dto.ListEmployeesResponse;
import com.MyAnimaLog.Veterinary.application.staff.ports.in.ListEmployeesUseCase;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.ClinicSuspendedException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.NotClinicMemberException;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.infrastructure.config.GlobalExceptionHandler;
import com.MyAnimaLog.Veterinary.infrastructure.security.ImportSecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ListEmployeesController.class)
@Import(GlobalExceptionHandler.class)
@ImportSecurityConfig
class ListEmployeesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListEmployeesUseCase listEmployeesUseCase;

    private UUID veterinaryId;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();
        when(listEmployeesUseCase.list(any(UUID.class), anyBoolean())).thenReturn(ListEmployeesResponse.builder()
                .employees(List.of(ListEmployeesResponse.Employee.builder()
                        .employeeId(UUID.randomUUID())
                        .userId(UUID.randomUUID())
                        .name("Ana Dueña")
                        .email("owner@clinica.com")
                        .role(EmployeeRole.OWNER)
                        .active(true)
                        .createdAt(LocalDateTime.now())
                        .self(true)
                        .build()))
                .build());
    }

    @Test
    void list_shouldReturn200AndExcludeRemovedEmployeesByDefault() throws Exception {
        mockMvc.perform(get("/api/veterinary/{veterinaryId}/employees", veterinaryId).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employees[0].name").value("Ana Dueña"))
                .andExpect(jsonPath("$.employees[0].role").value("OWNER"))
                .andExpect(jsonPath("$.employees[0].self").value(true))
                .andExpect(jsonPath("$.employees[0].documentNumber").doesNotExist());

        verify(listEmployeesUseCase).list(veterinaryId, false);
    }

    @Test
    void list_shouldPassIncludeInactiveThrough() throws Exception {
        mockMvc.perform(get("/api/veterinary/{veterinaryId}/employees", veterinaryId)
                        .param("includeInactive", "true")
                        .with(jwt()))
                .andExpect(status().isOk());

        verify(listEmployeesUseCase).list(eq(veterinaryId), eq(true));
    }

    @Test
    void list_shouldReturn403_whenTheCallerIsNoLongerAMember() throws Exception {
        when(listEmployeesUseCase.list(any(UUID.class), anyBoolean()))
                .thenThrow(new NotClinicMemberException());

        mockMvc.perform(get("/api/veterinary/{veterinaryId}/employees", veterinaryId).with(jwt()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_CLINIC_MEMBER"));
    }

    @Test
    void list_shouldReturn403_whenTheClinicIsSuspended() throws Exception {
        when(listEmployeesUseCase.list(any(UUID.class), anyBoolean()))
                .thenThrow(new ClinicSuspendedException());

        mockMvc.perform(get("/api/veterinary/{veterinaryId}/employees", veterinaryId).with(jwt()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CLINIC_SUSPENDED"));
    }

    @Test
    void list_shouldReturn401_whenNoTokenIsPresent() throws Exception {
        mockMvc.perform(get("/api/veterinary/{veterinaryId}/employees", veterinaryId))
                .andExpect(status().isUnauthorized());
    }
}

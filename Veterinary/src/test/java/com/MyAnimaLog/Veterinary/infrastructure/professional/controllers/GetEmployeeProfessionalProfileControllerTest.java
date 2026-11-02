package com.MyAnimaLog.Veterinary.infrastructure.professional.controllers;

import com.MyAnimaLog.Veterinary.application.professional.dto.EmployeeProfessionalProfileResponse;
import com.MyAnimaLog.Veterinary.application.professional.ports.in.GetEmployeeProfessionalProfileUseCase;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.ClinicContextRequiredException;
import com.MyAnimaLog.Veterinary.infrastructure.config.GlobalExceptionHandler;
import com.MyAnimaLog.Veterinary.infrastructure.security.ImportSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = GetEmployeeProfessionalProfileController.class)
@Import(GlobalExceptionHandler.class)
@ImportSecurityConfig
class GetEmployeeProfessionalProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetEmployeeProfessionalProfileUseCase getEmployeeProfessionalProfileUseCase;

    @Test
    void getProfile_shouldReturn200_whenCallerIsMember() throws Exception {
        UUID veterinaryId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        when(getEmployeeProfessionalProfileUseCase.getProfile(veterinaryId, employeeId)).thenReturn(
                EmployeeProfessionalProfileResponse.builder().exists(true).fullName("Juan Pérez").build());

        mockMvc.perform(get("/api/veterinary/{veterinaryId}/employees/{employeeId}/professional-profile", veterinaryId, employeeId)
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Juan Pérez"))
                .andExpect(jsonPath("$.documentNumber").doesNotExist());
    }

    @Test
    void getProfile_shouldReturn403_whenCallerIsNotMember() throws Exception {
        UUID veterinaryId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        when(getEmployeeProfessionalProfileUseCase.getProfile(veterinaryId, employeeId))
                .thenThrow(new ClinicContextRequiredException());

        mockMvc.perform(get("/api/veterinary/{veterinaryId}/employees/{employeeId}/professional-profile", veterinaryId, employeeId)
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void getProfile_shouldReturn401_whenNoTokenIsPresent() throws Exception {
        UUID veterinaryId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        mockMvc.perform(get("/api/veterinary/{veterinaryId}/employees/{employeeId}/professional-profile", veterinaryId, employeeId)
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}

package com.MyAnimaLog.Veterinary.infrastructure.clinic.controllers;

import com.MyAnimaLog.Veterinary.application.clinic.dto.GetMyVeterinariesResponse;
import com.MyAnimaLog.Veterinary.application.clinic.dto.MyVeterinarySummary;
import com.MyAnimaLog.Veterinary.application.clinic.ports.in.GetMyVeterinariesUseCase;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.infrastructure.config.GlobalExceptionHandler;
import com.MyAnimaLog.Veterinary.infrastructure.security.ImportSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = GetMyVeterinariesController.class)
@Import(GlobalExceptionHandler.class)
@ImportSecurityConfig
class GetMyVeterinariesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetMyVeterinariesUseCase getMyVeterinariesUseCase;

    @Test
    void getMyVeterinaries_shouldReturn200_withList_whenTokenIsPresent() throws Exception {
        MyVeterinarySummary summary = MyVeterinarySummary.builder()
                .veterinaryId(UUID.randomUUID())
                .name("Clínica El Bosque")
                .role(EmployeeRole.OWNER)
                .status(VeterinaryStatus.ACTIVE)
                .build();
        when(getMyVeterinariesUseCase.getMyVeterinaries())
                .thenReturn(GetMyVeterinariesResponse.builder().veterinaries(List.of(summary)).build());

        mockMvc.perform(get("/api/veterinary/me")
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.veterinaries[0].name").value("Clínica El Bosque"))
                .andExpect(jsonPath("$.veterinaries[0].role").value("OWNER"))
                .andExpect(jsonPath("$.veterinaries[0].status").value("ACTIVE"));
    }

    @Test
    void getMyVeterinaries_shouldReturn401_whenNoTokenIsPresent() throws Exception {
        mockMvc.perform(get("/api/veterinary/me"))
                .andExpect(status().isUnauthorized());
    }
}

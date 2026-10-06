package com.MyAnimaLog.Veterinary.infrastructure.clinic.controllers;

import com.MyAnimaLog.Veterinary.application.clinic.dto.GetVeterinaryMemberResponse;
import com.MyAnimaLog.Veterinary.application.clinic.ports.in.GetVeterinaryMemberUseCase;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
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

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GetVeterinaryMemberController.class)
@Import(GlobalExceptionHandler.class)
@ImportSecurityConfig
class GetVeterinaryMemberControllerTest {

    private static final String HEADER = "X-Internal-Api-Key";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetVeterinaryMemberUseCase getVeterinaryMemberUseCase;

    @Value("${app.security.internal-api-key}")
    private String internalApiKey;

    @Test
    void getMember_shouldReturn401_whenInternalApiKeyIsMissing() throws Exception {
        UUID veterinaryId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        mockMvc.perform(get("/internal/veterinaries/{veterinaryId}/members/{userId}", veterinaryId, userId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void getMember_shouldReturn401_whenInternalApiKeyIsWrong() throws Exception {
        UUID veterinaryId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        mockMvc.perform(get("/internal/veterinaries/{veterinaryId}/members/{userId}", veterinaryId, userId)
                        .header(HEADER, "wrong-key"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMember_shouldReturn200_whenInternalApiKeyIsCorrect() throws Exception {
        UUID veterinaryId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        GetVeterinaryMemberResponse response = GetVeterinaryMemberResponse.builder()
                .member(true)
                .employeeId(UUID.randomUUID())
                .role(EmployeeRole.VETERINARIAN)
                .active(true)
                .licensed(false)
                .veterinaryStatus(VeterinaryStatus.ACTIVE)
                .subscriptionStatus("ACTIVE")
                .build();
        when(getVeterinaryMemberUseCase.getMember(any(UUID.class), any(UUID.class))).thenReturn(response);

        mockMvc.perform(get("/internal/veterinaries/{veterinaryId}/members/{userId}", veterinaryId, userId)
                        .header(HEADER, internalApiKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.member").value(true))
                .andExpect(jsonPath("$.role").value("VETERINARIAN"))
                .andExpect(jsonPath("$.veterinaryStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.subscriptionStatus").value("ACTIVE"));
    }

    @Test
    void getMember_shouldReturn200WithMemberFalse_whenNotAnEmployee() throws Exception {
        UUID veterinaryId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        GetVeterinaryMemberResponse response = GetVeterinaryMemberResponse.builder()
                .member(false)
                .licensed(false)
                .veterinaryStatus(VeterinaryStatus.ACTIVE)
                .subscriptionStatus("NONE")
                .build();
        when(getVeterinaryMemberUseCase.getMember(any(UUID.class), any(UUID.class))).thenReturn(response);

        mockMvc.perform(get("/internal/veterinaries/{veterinaryId}/members/{userId}", veterinaryId, userId)
                        .header(HEADER, internalApiKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.member").value(false));
    }
}

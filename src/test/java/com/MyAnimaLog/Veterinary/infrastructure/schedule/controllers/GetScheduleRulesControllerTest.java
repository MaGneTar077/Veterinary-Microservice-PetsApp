package com.MyAnimaLog.Veterinary.infrastructure.schedule.controllers;

import com.MyAnimaLog.Veterinary.application.schedule.dto.GetScheduleRulesResponse;
import com.MyAnimaLog.Veterinary.application.schedule.ports.in.GetScheduleRulesUseCase;
import com.MyAnimaLog.Veterinary.domain.schedule.exceptions.InvalidScheduleRangeException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VeterinaryNotFoundException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.EmployeeNotFoundException;
import com.MyAnimaLog.Veterinary.infrastructure.config.GlobalExceptionHandler;
import com.MyAnimaLog.Veterinary.infrastructure.security.ImportSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GetScheduleRulesController.class)
@Import(GlobalExceptionHandler.class)
@ImportSecurityConfig
class GetScheduleRulesControllerTest {

    private static final String FROM = "2026-03-01T00:00:00-05:00";
    private static final String TO = "2026-03-15T00:00:00-05:00";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetScheduleRulesUseCase getScheduleRulesUseCase;

    @Value("${app.security.internal-api-key}")
    private String internalApiKey;

    private String url(UUID veterinaryId) {
        return "/internal/veterinaries/" + veterinaryId + "/schedule-rules?from=" + FROM + "&to=" + TO;
    }

    @Test
    void get_shouldReturn401_whenInternalApiKeyIsMissing() throws Exception {
        mockMvc.perform(get(url(UUID.randomUUID())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void get_shouldReturnTheJsonShapeCalendarExpects() throws Exception {
        UUID employeeId = UUID.randomUUID();
        when(getScheduleRulesUseCase.get(any(), any(), any(), any())).thenReturn(
                GetScheduleRulesResponse.builder()
                        .timezone("America/Bogota")
                        .businessHours(List.of(GetScheduleRulesResponse.BusinessHour.builder()
                                .dayOfWeek(1).opensAt("08:00").closesAt("17:00").build()))
                        .employeeHours(List.of(GetScheduleRulesResponse.EmployeeHour.builder()
                                .employeeId(employeeId).dayOfWeek(1).opensAt("09:00").closesAt("13:00").build()))
                        .blocks(List.of(GetScheduleRulesResponse.Block.builder()
                                .employeeId(null)
                                .startsAt(OffsetDateTime.of(2026, 3, 2, 0, 0, 0, 0, ZoneOffset.UTC))
                                .endsAt(OffsetDateTime.of(2026, 3, 3, 0, 0, 0, 0, ZoneOffset.UTC))
                                .reason("Festivo").build()))
                        .build());

        mockMvc.perform(get(url(UUID.randomUUID())).header("X-Internal-Api-Key", internalApiKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timezone").value("America/Bogota"))
                .andExpect(jsonPath("$.businessHours[0].dayOfWeek").value(1))
                .andExpect(jsonPath("$.businessHours[0].opensAt").value("08:00"))
                .andExpect(jsonPath("$.employeeHours[0].employeeId").value(employeeId.toString()))
                .andExpect(jsonPath("$.employeeHours[0].closesAt").value("13:00"))
                .andExpect(jsonPath("$.blocks[0].employeeId").doesNotExist())
                .andExpect(jsonPath("$.blocks[0].reason").value("Festivo"));
    }

    @Test
    void get_shouldReturn404_whenTheClinicDoesNotExist() throws Exception {
        when(getScheduleRulesUseCase.get(any(), any(), any(), any()))
                .thenThrow(new VeterinaryNotFoundException());

        mockMvc.perform(get(url(UUID.randomUUID())).header("X-Internal-Api-Key", internalApiKey))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("VETERINARY_NOT_FOUND"));
    }

    @Test
    void get_shouldReturn404_whenTheEmployeeIsNotActiveInThisClinic() throws Exception {
        when(getScheduleRulesUseCase.get(any(), any(), any(), any()))
                .thenThrow(new EmployeeNotFoundException());

        mockMvc.perform(get(url(UUID.randomUUID()) + "&employeeId=" + UUID.randomUUID())
                        .header("X-Internal-Api-Key", internalApiKey))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EMPLOYEE_NOT_FOUND"));
    }

    @Test
    void get_shouldReturn400_whenTheRangeIsInvalid() throws Exception {
        when(getScheduleRulesUseCase.get(any(), any(), any(), any()))
                .thenThrow(new InvalidScheduleRangeException("to must be after from"));

        mockMvc.perform(get("/internal/veterinaries/" + UUID.randomUUID() + "/schedule-rules")
                        .header("X-Internal-Api-Key", internalApiKey))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SCHEDULE_RANGE"));
    }
}

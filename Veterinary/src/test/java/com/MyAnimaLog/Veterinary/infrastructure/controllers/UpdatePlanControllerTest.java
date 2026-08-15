package com.MyAnimaLog.Veterinary.infrastructure.controllers;

import com.MyAnimaLog.Veterinary.application.dto.UpdatePlanRequest;
import com.MyAnimaLog.Veterinary.application.dto.UpdatePlanResponse;
import com.MyAnimaLog.Veterinary.application.ports.in.UpdatePlanUseCase;
import com.MyAnimaLog.Veterinary.domain.exceptions.NoActiveSubscriptionException;
import com.MyAnimaLog.Veterinary.domain.exceptions.VeterinaryNotFoundException;
import com.MyAnimaLog.Veterinary.infrastructure.config.GlobalExceptionHandler;
import com.MyAnimaLog.Veterinary.infrastructure.config.SecurityConfig;
import com.MyAnimaLog.Veterinary.infrastructure.controllers.UpdatePlanController;
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

import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = UpdatePlanController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityConfig.class
        )
)
@Import(GlobalExceptionHandler.class)
@WithMockUser
class UpdatePlanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UpdatePlanUseCase updatePlanUseCase;

    private UUID veterinaryId;
    private UpdatePlanResponse validResponse;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();

        validResponse = UpdatePlanResponse.builder()
                .id(UUID.randomUUID())
                .veterinaryId(veterinaryId)
                .plan("ENTERPRISE")
                .startDate(LocalDate.of(2026, 7, 1))
                .endDate(LocalDate.of(2028, 7, 1))
                .active(true)
                .build();
    }

    @Test
    void updatePlan_shouldReturn200_whenRequestIsValid() throws Exception {
        when(updatePlanUseCase.updatePlan(any(UUID.class), any(UpdatePlanRequest.class)))
                .thenReturn(validResponse);

        mockMvc.perform(patch("/api/veterinary/{veterinaryId}/subscription", veterinaryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                UpdatePlanRequest.builder()
                                        .plan("ENTERPRISE")
                                        .endDate(LocalDate.of(2028, 7, 1))
                                        .build()
                        ))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plan").value("ENTERPRISE"))
                .andExpect(jsonPath("$.endDate").value("2028-07-01"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void updatePlan_shouldReturn404_whenVeterinaryNotFound() throws Exception {
        when(updatePlanUseCase.updatePlan(any(UUID.class), any(UpdatePlanRequest.class)))
                .thenThrow(new VeterinaryNotFoundException());

        mockMvc.perform(patch("/api/veterinary/{veterinaryId}/subscription", veterinaryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(UpdatePlanRequest.builder().build()))
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Veterinary not found"));
    }

    @Test
    void updatePlan_shouldReturn404_whenNoActiveSubscription() throws Exception {
        when(updatePlanUseCase.updatePlan(any(UUID.class), any(UpdatePlanRequest.class)))
                .thenThrow(new NoActiveSubscriptionException());

        mockMvc.perform(patch("/api/veterinary/{veterinaryId}/subscription", veterinaryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(UpdatePlanRequest.builder().build()))
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Veterinary has no active subscription"));
    }
}
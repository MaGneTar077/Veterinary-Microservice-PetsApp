package com.MyAnimaLog.Veterinary.infrastructure.controllers;

import com.MyAnimaLog.Veterinary.application.dto.CreatePlanRequest;
import com.MyAnimaLog.Veterinary.application.dto.CreatePlanResponse;
import com.MyAnimaLog.Veterinary.application.ports.in.CreatePlanUseCase;
import com.MyAnimaLog.Veterinary.domain.exceptions.ActiveSubscriptionAlreadyExistsException;
import com.MyAnimaLog.Veterinary.domain.exceptions.VeterinaryNotActiveException;
import com.MyAnimaLog.Veterinary.domain.exceptions.VeterinaryNotFoundException;
import com.MyAnimaLog.Veterinary.infrastructure.config.GlobalExceptionHandler;
import com.MyAnimaLog.Veterinary.infrastructure.config.SecurityConfig;
import com.MyAnimaLog.Veterinary.infrastructure.controllers.CreatePlanController;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = CreatePlanController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityConfig.class
        )
)
@Import(GlobalExceptionHandler.class)
@WithMockUser
class CreatePlanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CreatePlanUseCase createPlanUseCase;

    private UUID veterinaryId;
    private CreatePlanRequest validRequest;
    private CreatePlanResponse validResponse;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();

        validRequest = CreatePlanRequest.builder()
                .plan("PRO")
                .startDate(LocalDate.of(2026, 7, 1))
                .endDate(LocalDate.of(2027, 7, 1))
                .build();

        validResponse = CreatePlanResponse.builder()
                .id(UUID.randomUUID())
                .veterinaryId(veterinaryId)
                .plan("PRO")
                .startDate(LocalDate.of(2026, 7, 1))
                .endDate(LocalDate.of(2027, 7, 1))
                .active(true)
                .build();
    }

    @Test
    void createPlan_shouldReturn201_whenRequestIsValid() throws Exception {
        when(createPlanUseCase.createPlan(any(CreatePlanRequest.class))).thenReturn(validResponse);

        mockMvc.perform(post("/api/veterinary/{veterinaryId}/subscription", veterinaryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.plan").value("PRO"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.startDate").value("2026-07-01"))
                .andExpect(jsonPath("$.endDate").value("2027-07-01"));
    }

    @Test
    void createPlan_shouldReturn404_whenVeterinaryNotFound() throws Exception {
        when(createPlanUseCase.createPlan(any(CreatePlanRequest.class)))
                .thenThrow(new VeterinaryNotFoundException());

        mockMvc.perform(post("/api/veterinary/{veterinaryId}/subscription", veterinaryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest))
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Veterinary not found"));
    }

    @Test
    void createPlan_shouldReturn409_whenVeterinaryIsInactive() throws Exception {
        when(createPlanUseCase.createPlan(any(CreatePlanRequest.class)))
                .thenThrow(new VeterinaryNotActiveException());

        mockMvc.perform(post("/api/veterinary/{veterinaryId}/subscription", veterinaryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest))
                        .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Veterinary is not active"));
    }

    @Test
    void createPlan_shouldReturn409_whenActiveSubscriptionAlreadyExists() throws Exception {
        when(createPlanUseCase.createPlan(any(CreatePlanRequest.class)))
                .thenThrow(new ActiveSubscriptionAlreadyExistsException());

        mockMvc.perform(post("/api/veterinary/{veterinaryId}/subscription", veterinaryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest))
                        .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Veterinary already has an active subscription"));
    }
}
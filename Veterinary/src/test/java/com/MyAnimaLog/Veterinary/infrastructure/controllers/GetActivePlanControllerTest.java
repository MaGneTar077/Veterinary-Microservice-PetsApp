package com.MyAnimaLog.Veterinary.infrastructure.controllers;

import com.MyAnimaLog.Veterinary.application.dto.GetActivePlanResponse;
import com.MyAnimaLog.Veterinary.application.ports.in.GetActivePlanUseCase;
import com.MyAnimaLog.Veterinary.domain.exceptions.NoActiveSubscriptionException;
import com.MyAnimaLog.Veterinary.domain.exceptions.VeterinaryNotFoundException;
import com.MyAnimaLog.Veterinary.infrastructure.config.GlobalExceptionHandler;
import com.MyAnimaLog.Veterinary.infrastructure.config.SecurityConfig;
import com.MyAnimaLog.Veterinary.infrastructure.controllers.GetActivePlanController;
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

import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = GetActivePlanController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityConfig.class
        )
)
@Import(GlobalExceptionHandler.class)
@WithMockUser
class GetActivePlanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetActivePlanUseCase getActivePlanUseCase;

    private UUID veterinaryId;
    private GetActivePlanResponse validResponse;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();

        validResponse = GetActivePlanResponse.builder()
                .id(UUID.randomUUID())
                .veterinaryId(veterinaryId)
                .plan("BASIC")
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 12, 31))
                .active(true)
                .build();
    }

    @Test
    void getActivePlan_shouldReturn200_whenSubscriptionIsActive() throws Exception {
        when(getActivePlanUseCase.getActivePlan(any(UUID.class))).thenReturn(validResponse);

        mockMvc.perform(get("/api/veterinary/{veterinaryId}/subscription/active", veterinaryId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.veterinaryId").value(veterinaryId.toString()))
                .andExpect(jsonPath("$.plan").value("BASIC"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.startDate").value("2026-01-01"))
                .andExpect(jsonPath("$.endDate").value("2026-12-31"));
    }

    @Test
    void getActivePlan_shouldReturn404_whenVeterinaryNotFound() throws Exception {
        when(getActivePlanUseCase.getActivePlan(any(UUID.class)))
                .thenThrow(new VeterinaryNotFoundException());

        mockMvc.perform(get("/api/veterinary/{veterinaryId}/subscription/active", veterinaryId)
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Veterinary not found"));
    }

    @Test
    void getActivePlan_shouldReturn404_whenNoActiveSubscription() throws Exception {
        when(getActivePlanUseCase.getActivePlan(any(UUID.class)))
                .thenThrow(new NoActiveSubscriptionException());

        mockMvc.perform(get("/api/veterinary/{veterinaryId}/subscription/active", veterinaryId)
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Veterinary has no active subscription"));
    }

    @Test
    void getActivePlan_shouldReturn404_whenSubscriptionIsExpired() throws Exception {
        when(getActivePlanUseCase.getActivePlan(any(UUID.class)))
                .thenThrow(new NoActiveSubscriptionException("Subscription has expired"));

        mockMvc.perform(get("/api/veterinary/{veterinaryId}/subscription/active", veterinaryId)
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Subscription has expired"));
    }
}
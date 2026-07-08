package com.MyAnimaLog.Veterinary.infrastructure.controllers;

import com.MyAnimaLog.Veterinary.application.dto.IsExpiredResponse;
import com.MyAnimaLog.Veterinary.application.ports.in.IsExpiredUseCase;
import com.MyAnimaLog.Veterinary.domain.exceptions.SubscriptionNotFoundException;
import com.MyAnimaLog.Veterinary.domain.exceptions.VeterinaryNotFoundException;
import com.MyAnimaLog.Veterinary.infrastructure.config.GlobalExceptionHandler;
import com.MyAnimaLog.Veterinary.infrastructure.config.SecurityConfig;
import com.MyAnimaLog.Veterinary.infrastructure.controllers.IsExpiredController;
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
        controllers = IsExpiredController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityConfig.class
        )
)
@Import(GlobalExceptionHandler.class)
@WithMockUser
class IsExpiredControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IsExpiredUseCase isExpiredUseCase;

    private UUID veterinaryId;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();
    }

    @Test
    void isExpired_shouldReturn200_withExpiredFalse_whenSubscriptionIsActive() throws Exception {
        IsExpiredResponse response = IsExpiredResponse.builder()
                .veterinaryId(veterinaryId)
                .plan("BASIC")
                .endDate(LocalDate.of(2026, 12, 31))
                .expired(false)
                .build();

        when(isExpiredUseCase.isExpired(any(UUID.class))).thenReturn(response);

        mockMvc.perform(get("/api/veterinary/{veterinaryId}/subscription/is-expired", veterinaryId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expired").value(false))
                .andExpect(jsonPath("$.plan").value("BASIC"))
                .andExpect(jsonPath("$.endDate").value("2026-12-31"));
    }

    @Test
    void isExpired_shouldReturn200_withExpiredTrue_whenSubscriptionIsExpired() throws Exception {
        IsExpiredResponse response = IsExpiredResponse.builder()
                .veterinaryId(veterinaryId)
                .plan("BASIC")
                .endDate(LocalDate.of(2025, 6, 30))
                .expired(true)
                .build();

        when(isExpiredUseCase.isExpired(any(UUID.class))).thenReturn(response);

        mockMvc.perform(get("/api/veterinary/{veterinaryId}/subscription/is-expired", veterinaryId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expired").value(true))
                .andExpect(jsonPath("$.endDate").value("2025-06-30"));
    }

    @Test
    void isExpired_shouldReturn404_whenVeterinaryNotFound() throws Exception {
        when(isExpiredUseCase.isExpired(any(UUID.class)))
                .thenThrow(new VeterinaryNotFoundException());

        mockMvc.perform(get("/api/veterinary/{veterinaryId}/subscription/is-expired", veterinaryId)
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Veterinary not found"));
    }

    @Test
    void isExpired_shouldReturn404_whenSubscriptionNotFound() throws Exception {
        when(isExpiredUseCase.isExpired(any(UUID.class)))
                .thenThrow(new SubscriptionNotFoundException());

        mockMvc.perform(get("/api/veterinary/{veterinaryId}/subscription/is-expired", veterinaryId)
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Subscription not found"));
    }
}
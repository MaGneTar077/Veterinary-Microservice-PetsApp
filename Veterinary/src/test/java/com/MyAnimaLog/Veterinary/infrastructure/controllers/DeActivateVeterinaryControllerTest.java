package com.MyAnimaLog.Veterinary.infrastructure.controllers;

import com.MyAnimaLog.Veterinary.application.dto.DeActivateVeterinaryResponse;
import com.MyAnimaLog.Veterinary.application.ports.in.DeActivateVeterinaryUseCase;
import com.MyAnimaLog.Veterinary.domain.exceptions.VeterinaryNotFoundException;
import com.MyAnimaLog.Veterinary.infrastructure.config.GlobalExceptionHandler;
import com.MyAnimaLog.Veterinary.infrastructure.config.SecurityConfig;
import com.MyAnimaLog.Veterinary.infrastructure.controllers.DeActivateVeterinaryController;
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

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = DeActivateVeterinaryController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityConfig.class
        )
)
@Import(GlobalExceptionHandler.class)
@WithMockUser
class DeActivateVeterinaryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DeActivateVeterinaryUseCase deActivateVeterinaryUseCase;

    private UUID veterinaryId;
    private DeActivateVeterinaryResponse validResponse;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();

        validResponse = DeActivateVeterinaryResponse.builder()
                .id(veterinaryId)
                .name("Clínica El Bosque")
                .active(false)
                .build();
    }

    @Test
    void deActivate_shouldReturn200_whenVeterinaryExists() throws Exception {
        when(deActivateVeterinaryUseCase.deActivate(any(UUID.class))).thenReturn(validResponse);

        mockMvc.perform(patch("/api/veterinary/{veterinaryId}/deactivate", veterinaryId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(veterinaryId.toString()))
                .andExpect(jsonPath("$.name").value("Clínica El Bosque"))
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void deActivate_shouldReturn404_whenVeterinaryNotFound() throws Exception {
        when(deActivateVeterinaryUseCase.deActivate(any(UUID.class)))
                .thenThrow(new VeterinaryNotFoundException());

        mockMvc.perform(patch("/api/veterinary/{veterinaryId}/deactivate", veterinaryId)
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Veterinary not found"));
    }
}
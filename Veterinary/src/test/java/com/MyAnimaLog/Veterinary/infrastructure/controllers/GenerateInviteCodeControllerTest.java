package com.MyAnimaLog.Veterinary.infrastructure.controllers;

import com.MyAnimaLog.Veterinary.application.dto.GenerateInviteCodeResponse;
import com.MyAnimaLog.Veterinary.application.ports.in.GenerateInviteCodeUseCase;
import com.MyAnimaLog.Veterinary.domain.exceptions.VeterinaryNotFoundException;
import com.MyAnimaLog.Veterinary.infrastructure.config.GlobalExceptionHandler;
import com.MyAnimaLog.Veterinary.infrastructure.config.SecurityConfig;
import com.MyAnimaLog.Veterinary.infrastructure.controllers.GenerateInviteCodeController;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = GenerateInviteCodeController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityConfig.class
        )
)
@Import(GlobalExceptionHandler.class)
@WithMockUser
class GenerateInviteCodeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GenerateInviteCodeUseCase generateInviteCodeUseCase;

    private UUID veterinaryId;
    private GenerateInviteCodeResponse validResponse;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();

        validResponse = GenerateInviteCodeResponse.builder()
                .id(veterinaryId)
                .inviteCode("VET-A3X9K2B7")
                .inviteLink("http://localhost:8082/api/veterinary/join?code=VET-A3X9K2B7")
                .build();
    }

    @Test
    void generateInviteCode_shouldReturn200_whenVeterinaryExists() throws Exception {
        when(generateInviteCodeUseCase.generateInviteCode(any())).thenReturn(validResponse);

        mockMvc.perform(post("/api/veterinary/{veterinaryId}/invite-code", veterinaryId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(veterinaryId.toString()))
                .andExpect(jsonPath("$.inviteCode").value("VET-A3X9K2B7"))
                .andExpect(jsonPath("$.inviteLink").isNotEmpty());
    }

    @Test
    void generateInviteCode_shouldReturn404_whenVeterinaryNotFound() throws Exception {
        when(generateInviteCodeUseCase.generateInviteCode(any()))
                .thenThrow(new VeterinaryNotFoundException());

        mockMvc.perform(post("/api/veterinary/{veterinaryId}/invite-code", veterinaryId)
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Veterinary not found"));
    }
}
package com.MyAnimaLog.Veterinary.infrastructure.patients.controllers;

import com.MyAnimaLog.Veterinary.application.patients.dto.UnlinkResponse;
import com.MyAnimaLog.Veterinary.application.patients.ports.in.UnlinkMyVeterinaryUseCase;
import com.MyAnimaLog.Veterinary.domain.patients.exceptions.UserNotLinkedException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VeterinaryNotFoundException;
import com.MyAnimaLog.Veterinary.infrastructure.config.GlobalExceptionHandler;
import com.MyAnimaLog.Veterinary.infrastructure.security.ImportSecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UnlinkMyVeterinaryController.class)
@Import(GlobalExceptionHandler.class)
@ImportSecurityConfig
class UnlinkMyVeterinaryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UnlinkMyVeterinaryUseCase unlinkMyVeterinaryUseCase;

    private UUID veterinaryId;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();
    }

    @Test
    void unlinkMine_shouldReturn200_andReportTheRevokedPets() throws Exception {
        UUID userId = UUID.randomUUID();
        when(unlinkMyVeterinaryUseCase.unlinkMine(any(UUID.class))).thenReturn(UnlinkResponse.builder()
                .userId(userId)
                .veterinaryId(veterinaryId)
                .revokedPetsCount(3)
                .message("User unlinked successfully")
                .build());

        mockMvc.perform(delete("/api/veterinary/me/linked-veterinaries/{veterinaryId}", veterinaryId)
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.veterinaryId").value(veterinaryId.toString()))
                .andExpect(jsonPath("$.revokedPetsCount").value(3));
    }

    @Test
    void unlinkMine_shouldReturn404_whenTheUserIsNotLinked() throws Exception {
        when(unlinkMyVeterinaryUseCase.unlinkMine(any(UUID.class)))
                .thenThrow(new UserNotLinkedException());

        mockMvc.perform(delete("/api/veterinary/me/linked-veterinaries/{veterinaryId}", veterinaryId)
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_LINKED"));
    }

    @Test
    void unlinkMine_shouldReturn404_whenTheClinicDoesNotExist() throws Exception {
        when(unlinkMyVeterinaryUseCase.unlinkMine(any(UUID.class)))
                .thenThrow(new VeterinaryNotFoundException());

        mockMvc.perform(delete("/api/veterinary/me/linked-veterinaries/{veterinaryId}", veterinaryId)
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("VETERINARY_NOT_FOUND"));
    }

    @Test
    void unlinkMine_shouldReturn401_whenNoTokenIsPresent() throws Exception {
        mockMvc.perform(delete("/api/veterinary/me/linked-veterinaries/{veterinaryId}", veterinaryId)
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}

package com.MyAnimaLog.Veterinary.infrastructure.controllers;

import com.MyAnimaLog.Veterinary.application.dto.UnlinkResponse;
import com.MyAnimaLog.Veterinary.application.ports.in.UnlinkUseCase;
import com.MyAnimaLog.Veterinary.domain.exceptions.UserNotLinkedException;
import com.MyAnimaLog.Veterinary.domain.exceptions.VeterinaryNotFoundException;
import com.MyAnimaLog.Veterinary.infrastructure.config.GlobalExceptionHandler;
import com.MyAnimaLog.Veterinary.infrastructure.config.SecurityConfig;
import com.MyAnimaLog.Veterinary.infrastructure.controllers.UnlinkController;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = UnlinkController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityConfig.class
        )
)
@Import(GlobalExceptionHandler.class)
@WithMockUser
class UnlinkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UnlinkUseCase unlinkUseCase;

    private UUID userId;
    private UUID veterinaryId;
    private UnlinkResponse validResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        veterinaryId = UUID.randomUUID();

        validResponse = UnlinkResponse.builder()
                .userId(userId)
                .veterinaryId(veterinaryId)
                .message("User unlinked successfully")
                .build();
    }

    @Test
    void unlink_shouldReturn200_whenLinkExists() throws Exception {
        when(unlinkUseCase.unlink(any(UUID.class), any(UUID.class))).thenReturn(validResponse);

        mockMvc.perform(delete("/api/veterinary/link/{veterinaryId}/user/{userId}",
                        veterinaryId, userId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.veterinaryId").value(veterinaryId.toString()))
                .andExpect(jsonPath("$.message").value("User unlinked successfully"));
    }

    @Test
    void unlink_shouldReturn404_whenVeterinaryNotFound() throws Exception {
        when(unlinkUseCase.unlink(any(UUID.class), any(UUID.class)))
                .thenThrow(new VeterinaryNotFoundException());

        mockMvc.perform(delete("/api/veterinary/link/{veterinaryId}/user/{userId}",
                        veterinaryId, userId)
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Veterinary not found"));
    }

    @Test
    void unlink_shouldReturn404_whenUserNotLinked() throws Exception {
        when(unlinkUseCase.unlink(any(UUID.class), any(UUID.class)))
                .thenThrow(new UserNotLinkedException());

        mockMvc.perform(delete("/api/veterinary/link/{veterinaryId}/user/{userId}",
                        veterinaryId, userId)
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User is not linked to this veterinary"));
    }
}
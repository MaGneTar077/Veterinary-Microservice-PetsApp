package com.MyAnimaLog.Veterinary.infrastructure.patients.controllers;

import com.MyAnimaLog.Veterinary.application.patients.dto.UnlinkResponse;
import com.MyAnimaLog.Veterinary.application.patients.ports.in.UnlinkUseCase;
import com.MyAnimaLog.Veterinary.domain.patients.exceptions.UserNotLinkedException;
import com.MyAnimaLog.Veterinary.domain.security.Permission;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.InsufficientPermissionException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VeterinaryNotFoundException;
import com.MyAnimaLog.Veterinary.infrastructure.config.GlobalExceptionHandler;
import com.MyAnimaLog.Veterinary.infrastructure.security.ImportSecurityConfig;
import com.MyAnimaLog.Veterinary.infrastructure.patients.controllers.UnlinkController;
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

@WebMvcTest(controllers = UnlinkController.class)
@Import(GlobalExceptionHandler.class)
@ImportSecurityConfig
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
                        .with(csrf())
                        .with(jwt()))
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
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Veterinary not found"));
    }

    @Test
    void unlink_shouldReturn404_whenUserNotLinked() throws Exception {
        when(unlinkUseCase.unlink(any(UUID.class), any(UUID.class)))
                .thenThrow(new UserNotLinkedException());

        mockMvc.perform(delete("/api/veterinary/link/{veterinaryId}/user/{userId}",
                        veterinaryId, userId)
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User is not linked to this veterinary"));
    }

    @Test
    void unlink_shouldReturn401_whenNoTokenIsPresent() throws Exception {
        mockMvc.perform(delete("/api/veterinary/link/{veterinaryId}/user/{userId}",
                        veterinaryId, userId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unlink_shouldReturn403_whenCallerIsNeitherSelfNorClinicStaffWithPermission() throws Exception {
        when(unlinkUseCase.unlink(any(UUID.class), any(UUID.class)))
                .thenThrow(new InsufficientPermissionException(Permission.CLINIC_CONFIGURE));

        mockMvc.perform(delete("/api/veterinary/link/{veterinaryId}/user/{userId}",
                        veterinaryId, userId)
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isForbidden());
    }
}
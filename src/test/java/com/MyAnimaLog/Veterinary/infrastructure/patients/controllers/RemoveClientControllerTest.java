package com.MyAnimaLog.Veterinary.infrastructure.patients.controllers;

import com.MyAnimaLog.Veterinary.application.patients.dto.UnlinkResponse;
import com.MyAnimaLog.Veterinary.application.patients.ports.in.RemoveClientUseCase;
import com.MyAnimaLog.Veterinary.domain.security.Permission;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.InsufficientPermissionException;
import com.MyAnimaLog.Veterinary.infrastructure.config.GlobalExceptionHandler;
import com.MyAnimaLog.Veterinary.infrastructure.security.ImportSecurityConfig;
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

@WebMvcTest(controllers = RemoveClientController.class)
@Import(GlobalExceptionHandler.class)
@ImportSecurityConfig
class RemoveClientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RemoveClientUseCase removeClientUseCase;

    @Test
    void remove_shouldReturn200() throws Exception {
        UUID veterinaryId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(removeClientUseCase.remove(any(), any())).thenReturn(UnlinkResponse.builder()
                .veterinaryId(veterinaryId).userId(userId).revokedPetsCount(1).build());

        mockMvc.perform(delete("/api/veterinary/{id}/clients/{userId}", veterinaryId, userId)
                        .with(csrf()).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revokedPetsCount").value(1));
    }

    @Test
    void remove_shouldReturn403_whenCallerLacksClinicConfigure() throws Exception {
        when(removeClientUseCase.remove(any(), any()))
                .thenThrow(new InsufficientPermissionException(Permission.CLINIC_CONFIGURE));

        mockMvc.perform(delete("/api/veterinary/{id}/clients/{userId}", UUID.randomUUID(), UUID.randomUUID())
                        .with(csrf()).with(jwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void remove_shouldReturn401_whenNoTokenIsPresent() throws Exception {
        mockMvc.perform(delete("/api/veterinary/{id}/clients/{userId}", UUID.randomUUID(), UUID.randomUUID())
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}

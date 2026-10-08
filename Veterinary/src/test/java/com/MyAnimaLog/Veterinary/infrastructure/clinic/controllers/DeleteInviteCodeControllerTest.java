package com.MyAnimaLog.Veterinary.infrastructure.clinic.controllers;

import com.MyAnimaLog.Veterinary.application.clinic.dto.DeleteInviteCodeResponse;
import com.MyAnimaLog.Veterinary.application.clinic.ports.in.DeleteInviteCodeUseCase;
import com.MyAnimaLog.Veterinary.domain.security.Permission;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.InsufficientPermissionException;
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

@WebMvcTest(controllers = DeleteInviteCodeController.class)
@Import(GlobalExceptionHandler.class)
@ImportSecurityConfig
class DeleteInviteCodeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DeleteInviteCodeUseCase deleteInviteCodeUseCase;

    private UUID veterinaryId;
    private DeleteInviteCodeResponse validResponse;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();
        validResponse = DeleteInviteCodeResponse.builder()
                .id(veterinaryId)
                .inviteCode(null)
                .inviteLink(null)
                .build();
    }

    @Test
    void deleteInviteCode_shouldReturn200_whenCallerHasClinicConfigure() throws Exception {
        when(deleteInviteCodeUseCase.deleteInviteCode(any(UUID.class))).thenReturn(validResponse);

        mockMvc.perform(delete("/api/veterinary/{veterinaryId}/invite-code", veterinaryId)
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inviteCode").isEmpty())
                .andExpect(jsonPath("$.inviteLink").isEmpty());
    }

    @Test
    void deleteInviteCode_shouldReturn404_whenVeterinaryNotFound() throws Exception {
        when(deleteInviteCodeUseCase.deleteInviteCode(any(UUID.class))).thenThrow(new VeterinaryNotFoundException());

        mockMvc.perform(delete("/api/veterinary/{veterinaryId}/invite-code", veterinaryId)
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteInviteCode_shouldReturn403_whenCallerLacksClinicConfigure() throws Exception {
        when(deleteInviteCodeUseCase.deleteInviteCode(any(UUID.class)))
                .thenThrow(new InsufficientPermissionException(Permission.CLINIC_CONFIGURE));

        mockMvc.perform(delete("/api/veterinary/{veterinaryId}/invite-code", veterinaryId)
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteInviteCode_shouldReturn401_whenNoTokenIsPresent() throws Exception {
        mockMvc.perform(delete("/api/veterinary/{veterinaryId}/invite-code", veterinaryId))
                .andExpect(status().isUnauthorized());
    }
}

package com.MyAnimaLog.Veterinary.infrastructure.clinic.controllers;

import com.MyAnimaLog.Veterinary.application.clinic.dto.DeleteLogoResponse;
import com.MyAnimaLog.Veterinary.application.clinic.ports.in.DeleteLogoUseCase;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.NotVeterinaryOwnerException;
import com.MyAnimaLog.Veterinary.infrastructure.config.GlobalExceptionHandler;
import com.MyAnimaLog.Veterinary.infrastructure.security.ImportSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = DeleteLogoController.class)
@Import(GlobalExceptionHandler.class)
@ImportSecurityConfig
class DeleteLogoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DeleteLogoUseCase deleteLogoUseCase;

    @Test
    void delete_shouldReturn200_whenCallerIsOwner() throws Exception {
        UUID veterinaryId = UUID.randomUUID();
        when(deleteLogoUseCase.deleteLogo(veterinaryId))
                .thenReturn(DeleteLogoResponse.builder().veterinaryId(veterinaryId).logoUrl(null).build());

        mockMvc.perform(delete("/api/veterinary/{veterinaryId}/logo", veterinaryId)
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logoUrl").isEmpty());
    }

    @Test
    void delete_shouldReturn403_whenCallerIsNotOwnerOrConfigure() throws Exception {
        UUID veterinaryId = UUID.randomUUID();
        when(deleteLogoUseCase.deleteLogo(veterinaryId)).thenThrow(new NotVeterinaryOwnerException());

        mockMvc.perform(delete("/api/veterinary/{veterinaryId}/logo", veterinaryId)
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_shouldReturn401_whenNoTokenIsPresent() throws Exception {
        UUID veterinaryId = UUID.randomUUID();

        mockMvc.perform(delete("/api/veterinary/{veterinaryId}/logo", veterinaryId)
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}

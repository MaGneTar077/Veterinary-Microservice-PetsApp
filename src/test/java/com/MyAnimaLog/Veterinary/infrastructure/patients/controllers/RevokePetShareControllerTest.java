package com.MyAnimaLog.Veterinary.infrastructure.patients.controllers;

import com.MyAnimaLog.Veterinary.application.patients.dto.RevokePetShareResponse;
import com.MyAnimaLog.Veterinary.application.patients.ports.in.RevokePetShareUseCase;
import com.MyAnimaLog.Veterinary.domain.patients.exceptions.PetNotOwnedException;
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

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = RevokePetShareController.class)
@Import(GlobalExceptionHandler.class)
@ImportSecurityConfig
class RevokePetShareControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RevokePetShareUseCase revokePetShareUseCase;

    private UUID veterinaryId;
    private UUID petId;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();
        petId = UUID.randomUUID();
    }

    private String url() {
        return "/api/veterinary/me/linked-veterinaries/" + veterinaryId + "/pets/" + petId;
    }

    @Test
    void revoke_shouldReturn200() throws Exception {
        when(revokePetShareUseCase.revoke(eq(veterinaryId), eq(petId)))
                .thenReturn(RevokePetShareResponse.builder()
                        .veterinaryId(veterinaryId)
                        .petId(petId)
                        .petName("Max")
                        .revokedAt(LocalDateTime.now())
                        .alreadyRevoked(false)
                        .build());

        mockMvc.perform(delete(url()).with(csrf()).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.petId").value(petId.toString()))
                .andExpect(jsonPath("$.petName").value("Max"))
                .andExpect(jsonPath("$.alreadyRevoked").value(false));
    }

    @Test
    void revoke_shouldReturn200_whenAlreadyRevoked() throws Exception {
        when(revokePetShareUseCase.revoke(any(), any()))
                .thenReturn(RevokePetShareResponse.builder()
                        .veterinaryId(veterinaryId)
                        .petId(petId)
                        .petName("Max")
                        .revokedAt(LocalDateTime.now().minusDays(3))
                        .alreadyRevoked(true)
                        .build());

        // Idempotente: revocar dos veces no es un error, el resultado pedido ya se cumple.
        mockMvc.perform(delete(url()).with(csrf()).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alreadyRevoked").value(true));
    }

    @Test
    void revoke_shouldReturn403_whenTheShareIsNotTheCallers() throws Exception {
        when(revokePetShareUseCase.revoke(any(), any())).thenThrow(new PetNotOwnedException());

        mockMvc.perform(delete(url()).with(csrf()).with(jwt()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PET_NOT_OWNED"));
    }

    @Test
    void revoke_shouldReturn404_whenTheClinicDoesNotExist() throws Exception {
        when(revokePetShareUseCase.revoke(any(), any())).thenThrow(new VeterinaryNotFoundException());

        mockMvc.perform(delete(url()).with(csrf()).with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("VETERINARY_NOT_FOUND"));
    }

    @Test
    void revoke_shouldReturn401_whenNoTokenIsPresent() throws Exception {
        mockMvc.perform(delete(url()).with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}

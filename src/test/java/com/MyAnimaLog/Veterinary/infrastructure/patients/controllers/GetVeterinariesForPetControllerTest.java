package com.MyAnimaLog.Veterinary.infrastructure.patients.controllers;

import com.MyAnimaLog.Veterinary.application.patients.dto.GetVeterinariesForPetResponse;
import com.MyAnimaLog.Veterinary.application.patients.ports.in.GetVeterinariesForPetUseCase;
import com.MyAnimaLog.Veterinary.domain.patients.exceptions.PetNotOwnedException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.ExternalServiceUnavailableException;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = GetVeterinariesForPetController.class)
@Import(GlobalExceptionHandler.class)
@ImportSecurityConfig
class GetVeterinariesForPetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetVeterinariesForPetUseCase getVeterinariesForPetUseCase;

    private UUID petId;
    private UUID veterinaryId;

    @BeforeEach
    void setUp() {
        petId = UUID.randomUUID();
        veterinaryId = UUID.randomUUID();
    }

    private String url() {
        return "/api/veterinary/me/pets/" + petId + "/veterinaries";
    }

    @Test
    void list_shouldReturn200_withTheClinicsThePetIsSharedWith() throws Exception {
        when(getVeterinariesForPetUseCase.list(eq(petId))).thenReturn(
                GetVeterinariesForPetResponse.builder()
                        .petId(petId)
                        .veterinaries(List.of(GetVeterinariesForPetResponse.SharedWith.builder()
                                .veterinaryId(veterinaryId)
                                .name("Clínica El Bosque")
                                .logoUrl("https://cdn.test/logo.png")
                                .city("Cartagena")
                                .grantedAt(LocalDateTime.now())
                                .build()))
                        .build());

        mockMvc.perform(get(url()).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.petId").value(petId.toString()))
                .andExpect(jsonPath("$.veterinaries[0].veterinaryId").value(veterinaryId.toString()))
                .andExpect(jsonPath("$.veterinaries[0].name").value("Clínica El Bosque"));
    }

    @Test
    void list_shouldReturn200_withAnEmptyList_whenThePetIsNotShared() throws Exception {
        when(getVeterinariesForPetUseCase.list(any()))
                .thenReturn(GetVeterinariesForPetResponse.builder()
                        .petId(petId)
                        .veterinaries(List.of())
                        .build());

        mockMvc.perform(get(url()).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.veterinaries").isEmpty());
    }

    @Test
    void list_shouldReturn403_whenCallerIsNotThePetOwner() throws Exception {
        when(getVeterinariesForPetUseCase.list(any())).thenThrow(new PetNotOwnedException());

        mockMvc.perform(get(url()).with(jwt()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PET_NOT_OWNED"));
    }

    @Test
    void list_shouldReturn503_whenPetsIsDown() throws Exception {
        when(getVeterinariesForPetUseCase.list(any()))
                .thenThrow(new ExternalServiceUnavailableException("Pets service lookup failed"));

        mockMvc.perform(get(url()).with(jwt()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("EXTERNAL_SERVICE_UNAVAILABLE"));
    }

    @Test
    void list_shouldReturn401_whenNoTokenIsPresent() throws Exception {
        mockMvc.perform(get(url()))
                .andExpect(status().isUnauthorized());
    }
}

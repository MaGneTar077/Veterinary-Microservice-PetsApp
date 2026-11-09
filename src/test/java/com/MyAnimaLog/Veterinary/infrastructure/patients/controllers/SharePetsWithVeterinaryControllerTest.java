package com.MyAnimaLog.Veterinary.infrastructure.patients.controllers;

import com.MyAnimaLog.Veterinary.application.patients.dto.SharePetsWithVeterinaryRequest;
import com.MyAnimaLog.Veterinary.application.patients.dto.SharePetsWithVeterinaryResponse;
import com.MyAnimaLog.Veterinary.application.patients.ports.in.SharePetsWithVeterinaryUseCase;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.ClinicNotActiveException;
import com.MyAnimaLog.Veterinary.domain.patients.exceptions.InvalidPetShareException;
import com.MyAnimaLog.Veterinary.domain.patients.exceptions.NotLinkedToVeterinaryException;
import com.MyAnimaLog.Veterinary.domain.patients.exceptions.PetNotOwnedException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.ExternalServiceUnavailableException;
import com.MyAnimaLog.Veterinary.infrastructure.config.GlobalExceptionHandler;
import com.MyAnimaLog.Veterinary.infrastructure.security.ImportSecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = SharePetsWithVeterinaryController.class)
@Import(GlobalExceptionHandler.class)
@ImportSecurityConfig
class SharePetsWithVeterinaryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SharePetsWithVeterinaryUseCase sharePetsWithVeterinaryUseCase;

    private UUID veterinaryId;
    private UUID petId;
    private SharePetsWithVeterinaryRequest request;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();
        petId = UUID.randomUUID();
        request = SharePetsWithVeterinaryRequest.builder().petIds(List.of(petId)).build();
    }

    private String url() {
        return "/api/veterinary/me/linked-veterinaries/" + veterinaryId + "/pets";
    }

    @Test
    void share_shouldReturn200_withTheSharedPets() throws Exception {
        when(sharePetsWithVeterinaryUseCase.share(eq(veterinaryId),
                any(SharePetsWithVeterinaryRequest.class)))
                .thenReturn(SharePetsWithVeterinaryResponse.builder()
                        .veterinaryId(veterinaryId)
                        .veterinaryName("Clínica El Bosque")
                        .sharedCount(1)
                        .pets(List.of(SharePetsWithVeterinaryResponse.SharedPet.builder()
                                .petId(petId)
                                .petName("Max")
                                .species("DOG")
                                .grantedAt(LocalDateTime.now())
                                .alreadyShared(false)
                                .build()))
                        .build());

        mockMvc.perform(post(url())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sharedCount").value(1))
                .andExpect(jsonPath("$.pets[0].petId").value(petId.toString()))
                .andExpect(jsonPath("$.pets[0].petName").value("Max"))
                .andExpect(jsonPath("$.pets[0].alreadyShared").value(false));
    }

    @Test
    void share_shouldReturn403_whenAnyPetBelongsToSomeoneElse() throws Exception {
        when(sharePetsWithVeterinaryUseCase.share(any(), any())).thenThrow(new PetNotOwnedException());

        mockMvc.perform(post(url())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PET_NOT_OWNED"));
    }

    @Test
    void share_shouldReturn409_whenTheUserIsNotLinkedToTheClinic() throws Exception {
        when(sharePetsWithVeterinaryUseCase.share(any(), any()))
                .thenThrow(new NotLinkedToVeterinaryException());

        mockMvc.perform(post(url())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NOT_LINKED_TO_VETERINARY"));
    }

    @Test
    void share_shouldReturn409_whenTheClinicIsNotActive() throws Exception {
        when(sharePetsWithVeterinaryUseCase.share(any(), any())).thenThrow(new ClinicNotActiveException());

        mockMvc.perform(post(url())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CLINIC_NOT_ACTIVE"));
    }

    @Test
    void share_shouldReturn400_whenPetIdsIsEmpty() throws Exception {
        when(sharePetsWithVeterinaryUseCase.share(any(), any()))
                .thenThrow(new InvalidPetShareException("petIds is required and cannot be empty"));

        mockMvc.perform(post(url())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"petIds\":[]}")
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void share_shouldReturn503_whenPetsIsDown() throws Exception {
        when(sharePetsWithVeterinaryUseCase.share(any(), any()))
                .thenThrow(new ExternalServiceUnavailableException("Pets service lookup failed"));

        mockMvc.perform(post(url())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("EXTERNAL_SERVICE_UNAVAILABLE"));
    }

    @Test
    void share_shouldReturn401_whenNoTokenIsPresent() throws Exception {
        mockMvc.perform(post(url())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}

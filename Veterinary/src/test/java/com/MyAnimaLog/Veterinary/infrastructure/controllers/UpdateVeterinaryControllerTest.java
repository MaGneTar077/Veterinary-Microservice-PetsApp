package com.MyAnimaLog.Veterinary.infrastructure.controllers;

import com.MyAnimaLog.Veterinary.application.dto.UpdateVeterinaryRequest;
import com.MyAnimaLog.Veterinary.application.dto.UpdateVeterinaryResponse;
import com.MyAnimaLog.Veterinary.application.ports.in.UpdateVeterinaryUseCase;
import com.MyAnimaLog.Veterinary.domain.exceptions.InvalidVeterinaryEmailException;
import com.MyAnimaLog.Veterinary.domain.exceptions.InvalidVeterinaryNameException;
import com.MyAnimaLog.Veterinary.domain.exceptions.VeterinaryEmailAlreadyExistsException;
import com.MyAnimaLog.Veterinary.domain.exceptions.VeterinaryNotFoundException;
import com.MyAnimaLog.Veterinary.infrastructure.config.GlobalExceptionHandler;
import com.MyAnimaLog.Veterinary.infrastructure.config.SecurityConfig;
import com.MyAnimaLog.Veterinary.infrastructure.controllers.UpdateVeterinaryController;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = UpdateVeterinaryController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityConfig.class
        )
)
@Import(GlobalExceptionHandler.class)
@WithMockUser
class UpdateVeterinaryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UpdateVeterinaryUseCase updateVeterinaryUseCase;

    private UUID veterinaryId;
    private UpdateVeterinaryResponse validResponse;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();

        validResponse = UpdateVeterinaryResponse.builder()
                .id(veterinaryId)
                .name("Clínica El Bosque Actualizada")
                .city("Cartagena")
                .phone("3009999999")
                .email("elbosque@veterinaria.com")
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void update_shouldReturn200_whenRequestIsValid() throws Exception {
        when(updateVeterinaryUseCase.update(any(UUID.class), any(UpdateVeterinaryRequest.class)))
                .thenReturn(validResponse);

        UpdateVeterinaryRequest body = UpdateVeterinaryRequest.builder()
                .name("Clínica El Bosque Actualizada")
                .phone("3009999999")
                .build();

        mockMvc.perform(patch("/api/veterinary/{veterinaryId}", veterinaryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Clínica El Bosque Actualizada"))
                .andExpect(jsonPath("$.phone").value("3009999999"))
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    void update_shouldReturn404_whenVeterinaryNotFound() throws Exception {
        when(updateVeterinaryUseCase.update(any(UUID.class), any(UpdateVeterinaryRequest.class)))
                .thenThrow(new VeterinaryNotFoundException());

        mockMvc.perform(patch("/api/veterinary/{veterinaryId}", veterinaryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(UpdateVeterinaryRequest.builder().build()))
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Veterinary not found"));
    }

    @Test
    void update_shouldReturn400_whenNameIsInvalid() throws Exception {
        when(updateVeterinaryUseCase.update(any(UUID.class), any(UpdateVeterinaryRequest.class)))
                .thenThrow(new InvalidVeterinaryNameException());

        mockMvc.perform(patch("/api/veterinary/{veterinaryId}", veterinaryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(UpdateVeterinaryRequest.builder().name("  ").build()))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Veterinary name is invalid"));
    }

    @Test
    void update_shouldReturn400_whenEmailIsInvalid() throws Exception {
        when(updateVeterinaryUseCase.update(any(UUID.class), any(UpdateVeterinaryRequest.class)))
                .thenThrow(new InvalidVeterinaryEmailException());

        mockMvc.perform(patch("/api/veterinary/{veterinaryId}", veterinaryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(UpdateVeterinaryRequest.builder().email("bad-email").build()))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Veterinary email is invalid"));
    }

    @Test
    void update_shouldReturn409_whenEmailBelongsToAnother() throws Exception {
        when(updateVeterinaryUseCase.update(any(UUID.class), any(UpdateVeterinaryRequest.class)))
                .thenThrow(new VeterinaryEmailAlreadyExistsException());

        mockMvc.perform(patch("/api/veterinary/{veterinaryId}", veterinaryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(UpdateVeterinaryRequest.builder().email("otro@vet.com").build()))
                        .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("A veterinary with this email already exists"));
    }
}
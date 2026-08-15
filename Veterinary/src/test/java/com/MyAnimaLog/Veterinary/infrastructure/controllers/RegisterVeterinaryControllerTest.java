package com.MyAnimaLog.Veterinary.infrastructure.controllers;

import com.MyAnimaLog.Veterinary.application.dto.RegisterVeterinaryRequest;
import com.MyAnimaLog.Veterinary.application.dto.RegisterVeterinaryResponse;
import com.MyAnimaLog.Veterinary.application.ports.in.RegisterVeterinaryUseCase;
import com.MyAnimaLog.Veterinary.domain.exceptions.InvalidVeterinaryEmailException;
import com.MyAnimaLog.Veterinary.domain.exceptions.InvalidVeterinaryNameException;
import com.MyAnimaLog.Veterinary.domain.exceptions.VeterinaryAlreadyExistsException;
import com.MyAnimaLog.Veterinary.domain.exceptions.VeterinaryEmailAlreadyExistsException;
import com.MyAnimaLog.Veterinary.infrastructure.config.GlobalExceptionHandler;
import com.MyAnimaLog.Veterinary.infrastructure.config.SecurityConfig;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = RegisterVeterinaryController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityConfig.class
        )
)
@Import(GlobalExceptionHandler.class)
@WithMockUser
class RegisterVeterinaryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RegisterVeterinaryUseCase registerVeterinaryUseCase;

    private RegisterVeterinaryRequest validRequest;
    private RegisterVeterinaryResponse validResponse;

    @BeforeEach
    void setUp() {
        validRequest = RegisterVeterinaryRequest.builder()
                .name("Clínica El Bosque")
                .city("Cartagena")
                .phone("3001234567")
                .email("elbosque@veterinaria.com")
                .build();

        validResponse = RegisterVeterinaryResponse.builder()
                .id(UUID.randomUUID())
                .name("Clínica El Bosque")
                .city("Cartagena")
                .phone("3001234567")
                .email("elbosque@veterinaria.com")
                .tenantId(UUID.randomUUID().toString())
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void register_shouldReturn201_whenRequestIsValid() throws Exception {
        when(registerVeterinaryUseCase.registerVeterinary(any(RegisterVeterinaryRequest.class)))
                .thenReturn(validResponse);

        mockMvc.perform(post("/api/veterinary/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Clínica El Bosque"))
                .andExpect(jsonPath("$.email").value("elbosque@veterinaria.com"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.tenantId").isNotEmpty())
                .andExpect(jsonPath("$.id").isNotEmpty());
    }

    @Test
    void register_shouldReturn400_whenNameIsInvalid() throws Exception {
        when(registerVeterinaryUseCase.registerVeterinary(any(RegisterVeterinaryRequest.class)))
                .thenThrow(new InvalidVeterinaryNameException());

        mockMvc.perform(post("/api/veterinary/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Veterinary name is invalid"));
    }

    @Test
    void register_shouldReturn400_whenEmailIsInvalid() throws Exception {
        when(registerVeterinaryUseCase.registerVeterinary(any(RegisterVeterinaryRequest.class)))
                .thenThrow(new InvalidVeterinaryEmailException());

        mockMvc.perform(post("/api/veterinary/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Veterinary email is invalid"));
    }

    @Test
    void register_shouldReturn409_whenNameAlreadyExists() throws Exception {
        when(registerVeterinaryUseCase.registerVeterinary(any(RegisterVeterinaryRequest.class)))
                .thenThrow(new VeterinaryAlreadyExistsException());

        mockMvc.perform(post("/api/veterinary/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest))
                        .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Veterinary already exists"));
    }

    @Test
    void register_shouldReturn409_whenEmailAlreadyExists() throws Exception {
        when(registerVeterinaryUseCase.registerVeterinary(any(RegisterVeterinaryRequest.class)))
                .thenThrow(new VeterinaryEmailAlreadyExistsException());

        mockMvc.perform(post("/api/veterinary/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest))
                        .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("A veterinary with this email already exists"));
    }
}
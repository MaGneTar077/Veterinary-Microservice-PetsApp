package com.MyAnimaLog.Veterinary.infrastructure.clinic.controllers;

import com.MyAnimaLog.Veterinary.application.clinic.dto.RegisterVeterinaryRequest;
import com.MyAnimaLog.Veterinary.application.clinic.dto.RegisterVeterinaryResponse;
import com.MyAnimaLog.Veterinary.application.clinic.ports.in.RegisterVeterinaryUseCase;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.EmailNotVerifiedException;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.InvalidNitException;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.InvalidVeterinaryEmailException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.InvalidVeterinaryNameException;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.TooManyOwnedVeterinariesException;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.VeterinaryAlreadyExistsException;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.VeterinaryEmailAlreadyExistsException;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.VeterinaryNitAlreadyExistsException;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = RegisterVeterinaryController.class)
@Import(GlobalExceptionHandler.class)
@ImportSecurityConfig
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
                .nit("123456789-6")
                .build();

        validResponse = RegisterVeterinaryResponse.builder()
                .id(UUID.randomUUID())
                .name("Clínica El Bosque")
                .city("Cartagena")
                .phone("3001234567")
                .email("elbosque@veterinaria.com")
                .tenantId(UUID.randomUUID().toString())
                .active(false)
                .status(VeterinaryStatus.PENDING_DOCUMENTS)
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
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Clínica El Bosque"))
                .andExpect(jsonPath("$.email").value("elbosque@veterinaria.com"))
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.status").value("PENDING_DOCUMENTS"))
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
                        .with(csrf())
                        .with(jwt()))
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
                        .with(csrf())
                        .with(jwt()))
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
                        .with(csrf())
                        .with(jwt()))
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
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("A veterinary with this email already exists"));
    }

    @Test
    void register_shouldReturn400_whenNitIsInvalid() throws Exception {
        when(registerVeterinaryUseCase.registerVeterinary(any(RegisterVeterinaryRequest.class)))
                .thenThrow(new InvalidNitException());

        mockMvc.perform(post("/api/veterinary/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest))
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_shouldReturn409_whenNitAlreadyExists() throws Exception {
        when(registerVeterinaryUseCase.registerVeterinary(any(RegisterVeterinaryRequest.class)))
                .thenThrow(new VeterinaryNitAlreadyExistsException());

        mockMvc.perform(post("/api/veterinary/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest))
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isConflict());
    }

    @Test
    void register_shouldReturn403_whenCallerEmailIsNotVerified() throws Exception {
        when(registerVeterinaryUseCase.registerVeterinary(any(RegisterVeterinaryRequest.class)))
                .thenThrow(new EmailNotVerifiedException());

        mockMvc.perform(post("/api/veterinary/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest))
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void register_shouldReturn409_whenCallerAlreadyOwnsThreeNonFinalClinics() throws Exception {
        when(registerVeterinaryUseCase.registerVeterinary(any(RegisterVeterinaryRequest.class)))
                .thenThrow(new TooManyOwnedVeterinariesException());

        mockMvc.perform(post("/api/veterinary/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest))
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isConflict());
    }

    @Test
    void register_shouldReturn401_whenNoTokenIsPresent() throws Exception {
        mockMvc.perform(post("/api/veterinary/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest))
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}
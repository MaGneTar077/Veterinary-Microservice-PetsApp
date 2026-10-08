package com.MyAnimaLog.Veterinary.infrastructure.staff.controllers;

import com.MyAnimaLog.Veterinary.application.staff.dto.CreateEmployeeRequest;
import com.MyAnimaLog.Veterinary.application.staff.dto.CreateEmployeeResponse;
import com.MyAnimaLog.Veterinary.application.staff.ports.in.CreateEmployeeUseCase;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.EmployeeAlreadyExistsException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.InvalidEmployeeRoleException;
import com.MyAnimaLog.Veterinary.domain.security.Permission;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.InsufficientPermissionException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.TenantMismatchException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VeterinaryNotActiveException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VeterinaryNotFoundException;
import com.MyAnimaLog.Veterinary.infrastructure.config.GlobalExceptionHandler;
import com.MyAnimaLog.Veterinary.infrastructure.security.ImportSecurityConfig;
import com.MyAnimaLog.Veterinary.infrastructure.staff.controllers.CreateEmployeeController;
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

@WebMvcTest(controllers = CreateEmployeeController.class)
@Import(GlobalExceptionHandler.class)
@ImportSecurityConfig
class CreateEmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CreateEmployeeUseCase createEmployeeUseCase;

    private UUID veterinaryId;
    private UUID userId;
    private CreateEmployeeRequest validRequest;
    private CreateEmployeeResponse validResponse;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();
        userId = UUID.randomUUID();

        validRequest = CreateEmployeeRequest.builder()
                .veterinaryId(veterinaryId)
                .userId(userId)
                .role(EmployeeRole.VETERINARIAN)
                .build();

        validResponse = CreateEmployeeResponse.builder()
                .id(UUID.randomUUID())
                .veterinaryId(veterinaryId)
                .userId(userId)
                .role(EmployeeRole.VETERINARIAN)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void create_shouldReturn201_whenRequestIsValid() throws Exception {
        when(createEmployeeUseCase.create(any(CreateEmployeeRequest.class))).thenReturn(validResponse);

        mockMvc.perform(post("/api/veterinary/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest))
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.veterinaryId").value(veterinaryId.toString()))
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.role").value("VETERINARIAN"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void create_shouldReturn400_whenRoleIsInvalid() throws Exception {
        when(createEmployeeUseCase.create(any(CreateEmployeeRequest.class)))
                .thenThrow(new InvalidEmployeeRoleException());

        mockMvc.perform(post("/api/veterinary/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest))
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Employee role is invalid"));
    }

    @Test
    void create_shouldReturn400_whenRoleIsOwner() throws Exception {
        when(createEmployeeUseCase.create(any(CreateEmployeeRequest.class)))
                .thenThrow(new InvalidEmployeeRoleException("Cannot assign the OWNER role through this endpoint"));

        mockMvc.perform(post("/api/veterinary/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest))
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot assign the OWNER role through this endpoint"));
    }

    @Test
    void create_shouldReturn404_whenVeterinaryNotFound() throws Exception {
        when(createEmployeeUseCase.create(any(CreateEmployeeRequest.class)))
                .thenThrow(new VeterinaryNotFoundException());

        mockMvc.perform(post("/api/veterinary/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest))
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Veterinary not found"));
    }

    @Test
    void create_shouldReturn409_whenVeterinaryIsInactive() throws Exception {
        when(createEmployeeUseCase.create(any(CreateEmployeeRequest.class)))
                .thenThrow(new VeterinaryNotActiveException());

        mockMvc.perform(post("/api/veterinary/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest))
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Veterinary is not active"));
    }

    @Test
    void create_shouldReturn409_whenEmployeeAlreadyExists() throws Exception {
        when(createEmployeeUseCase.create(any(CreateEmployeeRequest.class)))
                .thenThrow(new EmployeeAlreadyExistsException());

        mockMvc.perform(post("/api/veterinary/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest))
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Employee already exists in this veterinary"));
    }

    @Test
    void create_shouldReturn401_whenNoTokenIsPresent() throws Exception {
        mockMvc.perform(post("/api/veterinary/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest))
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_shouldReturn403_whenCallerLacksStaffManage() throws Exception {
        when(createEmployeeUseCase.create(any(CreateEmployeeRequest.class)))
                .thenThrow(new InsufficientPermissionException(Permission.STAFF_MANAGE));

        mockMvc.perform(post("/api/veterinary/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest))
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_shouldReturn403_whenCallerBelongsToAnotherVeterinary() throws Exception {
        when(createEmployeeUseCase.create(any(CreateEmployeeRequest.class)))
                .thenThrow(new TenantMismatchException());

        mockMvc.perform(post("/api/veterinary/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest))
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isForbidden());
    }
}
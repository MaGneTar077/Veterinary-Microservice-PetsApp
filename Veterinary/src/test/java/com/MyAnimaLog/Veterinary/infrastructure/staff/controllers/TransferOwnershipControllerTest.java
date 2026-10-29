package com.MyAnimaLog.Veterinary.infrastructure.staff.controllers;

import com.MyAnimaLog.Veterinary.application.staff.dto.TransferOwnershipRequest;
import com.MyAnimaLog.Veterinary.application.staff.dto.TransferOwnershipResponse;
import com.MyAnimaLog.Veterinary.application.staff.ports.in.TransferOwnershipUseCase;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.NotVeterinaryOwnerException;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.EmployeeNotFoundException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.InvalidOwnershipTransferException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.OwnershipTransferConflictException;
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
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TransferOwnershipController.class)
@Import(GlobalExceptionHandler.class)
@ImportSecurityConfig
class TransferOwnershipControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TransferOwnershipUseCase transferOwnershipUseCase;

    private UUID veterinaryId;
    private UUID newOwnerEmployeeId;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();
        newOwnerEmployeeId = UUID.randomUUID();
    }

    private ResultActions perform() throws Exception {
        return mockMvc.perform(post("/api/veterinary/{veterinaryId}/ownership-transfer", veterinaryId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(TransferOwnershipRequest.builder()
                        .newOwnerEmployeeId(newOwnerEmployeeId)
                        .build()))
                .with(csrf())
                .with(jwt()));
    }

    @Test
    void transfer_shouldReturn200WithBothParties() throws Exception {
        UUID previousOwnerEmployeeId = UUID.randomUUID();
        when(transferOwnershipUseCase.transfer(any(UUID.class), any(TransferOwnershipRequest.class)))
                .thenReturn(TransferOwnershipResponse.builder()
                        .veterinaryId(veterinaryId)
                        .previousOwner(TransferOwnershipResponse.Party.builder()
                                .employeeId(previousOwnerEmployeeId)
                                .userId(UUID.randomUUID())
                                .role(EmployeeRole.ADMIN)
                                .build())
                        .newOwner(TransferOwnershipResponse.Party.builder()
                                .employeeId(newOwnerEmployeeId)
                                .userId(UUID.randomUUID())
                                .role(EmployeeRole.OWNER)
                                .build())
                        .legalRepresentativeChanged(false)
                        .build());

        perform()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.previousOwner.role").value("ADMIN"))
                .andExpect(jsonPath("$.newOwner.employeeId").value(newOwnerEmployeeId.toString()))
                .andExpect(jsonPath("$.newOwner.role").value("OWNER"))
                .andExpect(jsonPath("$.legalRepresentativeChanged").value(false));
    }

    @Test
    void transfer_shouldReturn403_whenTheCallerIsNotTheOwner() throws Exception {
        when(transferOwnershipUseCase.transfer(any(UUID.class), any(TransferOwnershipRequest.class)))
                .thenThrow(new NotVeterinaryOwnerException());

        perform()
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_VETERINARY_OWNER"));
    }

    @Test
    void transfer_shouldReturn400_whenTheTargetIsNotUsable() throws Exception {
        when(transferOwnershipUseCase.transfer(any(UUID.class), any(TransferOwnershipRequest.class)))
                .thenThrow(new InvalidOwnershipTransferException("newOwnerEmployeeId is required"));

        perform()
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void transfer_shouldReturn404_whenTheTargetEmployeeIsFromAnotherClinic() throws Exception {
        when(transferOwnershipUseCase.transfer(any(UUID.class), any(TransferOwnershipRequest.class)))
                .thenThrow(new EmployeeNotFoundException());

        perform()
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EMPLOYEE_NOT_FOUND"));
    }

    @Test
    void transfer_shouldReturn409_whenAnotherTransferWonTheRace() throws Exception {
        when(transferOwnershipUseCase.transfer(any(UUID.class), any(TransferOwnershipRequest.class)))
                .thenThrow(new OwnershipTransferConflictException());

        perform()
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OWNERSHIP_TRANSFER_CONFLICT"));
    }

    @Test
    void transfer_shouldReturn401_whenNoTokenIsPresent() throws Exception {
        mockMvc.perform(post("/api/veterinary/{veterinaryId}/ownership-transfer", veterinaryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(TransferOwnershipRequest.builder()
                                .newOwnerEmployeeId(newOwnerEmployeeId)
                                .build()))
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}

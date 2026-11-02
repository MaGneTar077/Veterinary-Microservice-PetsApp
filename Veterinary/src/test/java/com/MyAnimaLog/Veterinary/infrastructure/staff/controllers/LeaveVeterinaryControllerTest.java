package com.MyAnimaLog.Veterinary.infrastructure.staff.controllers;

import com.MyAnimaLog.Veterinary.application.staff.dto.LeaveVeterinaryResponse;
import com.MyAnimaLog.Veterinary.application.staff.ports.in.LeaveVeterinaryUseCase;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.NotClinicMemberException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.OwnerCannotLeaveException;
import com.MyAnimaLog.Veterinary.infrastructure.config.GlobalExceptionHandler;
import com.MyAnimaLog.Veterinary.infrastructure.security.ImportSecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = LeaveVeterinaryController.class)
@Import(GlobalExceptionHandler.class)
@ImportSecurityConfig
class LeaveVeterinaryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LeaveVeterinaryUseCase leaveVeterinaryUseCase;

    private UUID veterinaryId;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();
    }

    private ResultActions perform() throws Exception {
        return mockMvc.perform(post("/api/veterinary/{veterinaryId}/leave", veterinaryId)
                .with(csrf())
                .with(jwt()));
    }

    @Test
    void leave_shouldReturn200WithTheRemovalDate() throws Exception {
        UUID employeeId = UUID.randomUUID();
        when(leaveVeterinaryUseCase.leave(any(UUID.class))).thenReturn(LeaveVeterinaryResponse.builder()
                .employeeId(employeeId)
                .veterinaryId(veterinaryId)
                .removedAt(LocalDateTime.now())
                .build());

        perform()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeId").value(employeeId.toString()))
                .andExpect(jsonPath("$.removedAt").exists());
    }

    @Test
    void leave_shouldReturn409_whenTheCallerIsTheOwner() throws Exception {
        when(leaveVeterinaryUseCase.leave(any(UUID.class))).thenThrow(new OwnerCannotLeaveException());

        perform()
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OWNER_MUST_TRANSFER_OWNERSHIP"));
    }

    @Test
    void leave_shouldReturn403_whenTheCallerIsNoLongerAMember() throws Exception {
        when(leaveVeterinaryUseCase.leave(any(UUID.class))).thenThrow(new NotClinicMemberException());

        perform()
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_CLINIC_MEMBER"));
    }

    @Test
    void leave_shouldReturn401_whenNoTokenIsPresent() throws Exception {
        mockMvc.perform(post("/api/veterinary/{veterinaryId}/leave", veterinaryId).with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}

package com.MyAnimaLog.Veterinary.infrastructure.verification.controllers;

import com.MyAnimaLog.Veterinary.application.verification.dto.GetClinicVerificationResponse;
import com.MyAnimaLog.Veterinary.application.verification.ports.in.GetClinicVerificationUseCase;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.NotVeterinaryOwnerException;
import com.MyAnimaLog.Veterinary.domain.verification.enums.VerificationRequestStatus;
import com.MyAnimaLog.Veterinary.infrastructure.config.GlobalExceptionHandler;
import com.MyAnimaLog.Veterinary.infrastructure.security.ImportSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = GetClinicVerificationController.class)
@Import(GlobalExceptionHandler.class)
@ImportSecurityConfig
class GetClinicVerificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetClinicVerificationUseCase getClinicVerificationUseCase;

    @Test
    void getVerification_shouldReturn200_whenCallerIsOwner() throws Exception {
        UUID veterinaryId = UUID.randomUUID();
        when(getClinicVerificationUseCase.getVerification(veterinaryId)).thenReturn(
                GetClinicVerificationResponse.builder()
                        .veterinaryId(veterinaryId)
                        .veterinaryStatus(VeterinaryStatus.PENDING_DOCUMENTS)
                        .requestStatus(VerificationRequestStatus.DRAFT)
                        .attemptNumber(1)
                        .maxAttempts(3)
                        .attemptsRemaining(2)
                        .documents(List.of())
                        .missingRequired(List.of("RUT"))
                        .canEdit(true)
                        .canSubmit(false)
                        .build());

        mockMvc.perform(get("/api/veterinary/{veterinaryId}/verification", veterinaryId)
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestStatus").value("DRAFT"))
                .andExpect(jsonPath("$.canSubmit").value(false));
    }

    @Test
    void getVerification_shouldReturn403_whenCallerIsNotOwner() throws Exception {
        UUID veterinaryId = UUID.randomUUID();
        when(getClinicVerificationUseCase.getVerification(veterinaryId)).thenThrow(new NotVeterinaryOwnerException());

        mockMvc.perform(get("/api/veterinary/{veterinaryId}/verification", veterinaryId)
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void getVerification_shouldReturn401_whenNoTokenIsPresent() throws Exception {
        UUID veterinaryId = UUID.randomUUID();

        mockMvc.perform(get("/api/veterinary/{veterinaryId}/verification", veterinaryId)
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}

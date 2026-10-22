package com.MyAnimaLog.Veterinary.infrastructure.professional.controllers;

import com.MyAnimaLog.Veterinary.application.professional.dto.SubmitProfessionalProfileResponse;
import com.MyAnimaLog.Veterinary.application.professional.ports.in.SubmitProfessionalProfileUseCase;
import com.MyAnimaLog.Veterinary.domain.verification.enums.VerificationRequestStatus;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.MissingRequiredDocumentsException;
import com.MyAnimaLog.Veterinary.infrastructure.config.GlobalExceptionHandler;
import com.MyAnimaLog.Veterinary.infrastructure.security.ImportSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = SubmitProfessionalProfileController.class)
@Import(GlobalExceptionHandler.class)
@ImportSecurityConfig
class SubmitProfessionalProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SubmitProfessionalProfileUseCase submitProfessionalProfileUseCase;

    @Test
    void submit_shouldReturn200_whenValid() throws Exception {
        when(submitProfessionalProfileUseCase.submit()).thenReturn(
                SubmitProfessionalProfileResponse.builder().status(VerificationRequestStatus.UNDER_REVIEW).build());

        mockMvc.perform(post("/api/veterinary/me/professional-profile/submit")
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNDER_REVIEW"));
    }

    @Test
    void submit_shouldReturn400_whenMissingRequiredDocuments() throws Exception {
        when(submitProfessionalProfileUseCase.submit())
                .thenThrow(new MissingRequiredDocumentsException("Missing required: PROFESSIONAL_CARD"));

        mockMvc.perform(post("/api/veterinary/me/professional-profile/submit")
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MISSING_REQUIRED_DOCUMENTS"));
    }

    @Test
    void submit_shouldReturn401_whenNoTokenIsPresent() throws Exception {
        mockMvc.perform(post("/api/veterinary/me/professional-profile/submit")
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}

package com.MyAnimaLog.Veterinary.infrastructure.professional.controllers;

import com.MyAnimaLog.Veterinary.application.professional.dto.ProfessionalProfileResponse;
import com.MyAnimaLog.Veterinary.application.professional.ports.in.GetMyProfessionalProfileUseCase;
import com.MyAnimaLog.Veterinary.domain.professional.enums.LicenseStatus;
import com.MyAnimaLog.Veterinary.domain.professional.exceptions.ProfessionalProfileNotFoundException;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = GetMyProfessionalProfileController.class)
@Import(GlobalExceptionHandler.class)
@ImportSecurityConfig
class GetMyProfessionalProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetMyProfessionalProfileUseCase getMyProfessionalProfileUseCase;

    @Test
    void getMyProfile_shouldReturn200_whenProfileExists() throws Exception {
        when(getMyProfessionalProfileUseCase.getMyProfile()).thenReturn(
                ProfessionalProfileResponse.builder().fullName("Juan Pérez").licenseStatus(LicenseStatus.NOT_SUBMITTED).build());

        mockMvc.perform(get("/api/veterinary/me/professional-profile")
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Juan Pérez"));
    }

    @Test
    void getMyProfile_shouldReturn404_whenProfileDoesNotExist() throws Exception {
        when(getMyProfessionalProfileUseCase.getMyProfile()).thenThrow(new ProfessionalProfileNotFoundException());

        mockMvc.perform(get("/api/veterinary/me/professional-profile")
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROFESSIONAL_PROFILE_NOT_FOUND"));
    }

    @Test
    void getMyProfile_shouldReturn401_whenNoTokenIsPresent() throws Exception {
        mockMvc.perform(get("/api/veterinary/me/professional-profile")
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}

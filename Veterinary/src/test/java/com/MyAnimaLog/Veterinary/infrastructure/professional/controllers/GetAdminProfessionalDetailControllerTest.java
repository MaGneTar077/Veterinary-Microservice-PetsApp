package com.MyAnimaLog.Veterinary.infrastructure.professional.controllers;

import com.MyAnimaLog.Veterinary.application.professional.dto.AdminProfessionalDetailResponse;
import com.MyAnimaLog.Veterinary.application.professional.ports.in.GetAdminProfessionalDetailUseCase;
import com.MyAnimaLog.Veterinary.domain.professional.exceptions.ProfessionalProfileNotFoundException;
import com.MyAnimaLog.Veterinary.infrastructure.config.GlobalExceptionHandler;
import com.MyAnimaLog.Veterinary.infrastructure.security.ImportSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = GetAdminProfessionalDetailController.class)
@Import(GlobalExceptionHandler.class)
@ImportSecurityConfig
class GetAdminProfessionalDetailControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetAdminProfessionalDetailUseCase getAdminProfessionalDetailUseCase;

    @Test
    void getDetail_shouldReturn200_whenCallerIsPlatformAdmin() throws Exception {
        UUID profileId = UUID.randomUUID();
        when(getAdminProfessionalDetailUseCase.getDetail(any(UUID.class)))
                .thenReturn(AdminProfessionalDetailResponse.builder().id(profileId).fullName("Juan Pérez").build());

        mockMvc.perform(get("/api/veterinary/admin/professionals/{id}", profileId)
                        .with(csrf())
                        .with(jwt().authorities(new SimpleGrantedAuthority("PLATFORM_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Juan Pérez"));
    }

    @Test
    void getDetail_shouldReturn404_whenProfileNotFound() throws Exception {
        UUID profileId = UUID.randomUUID();
        when(getAdminProfessionalDetailUseCase.getDetail(any(UUID.class))).thenThrow(new ProfessionalProfileNotFoundException());

        mockMvc.perform(get("/api/veterinary/admin/professionals/{id}", profileId)
                        .with(csrf())
                        .with(jwt().authorities(new SimpleGrantedAuthority("PLATFORM_ADMIN"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void getDetail_shouldReturn403_whenCallerLacksPlatformAdminAuthority() throws Exception {
        UUID profileId = UUID.randomUUID();
        mockMvc.perform(get("/api/veterinary/admin/professionals/{id}", profileId)
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void getDetail_shouldReturn401_whenNoTokenIsPresent() throws Exception {
        UUID profileId = UUID.randomUUID();
        mockMvc.perform(get("/api/veterinary/admin/professionals/{id}", profileId)
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}

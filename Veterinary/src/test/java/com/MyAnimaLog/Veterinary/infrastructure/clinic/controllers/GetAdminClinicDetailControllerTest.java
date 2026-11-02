package com.MyAnimaLog.Veterinary.infrastructure.clinic.controllers;

import com.MyAnimaLog.Veterinary.application.clinic.dto.AdminClinicDetailResponse;
import com.MyAnimaLog.Veterinary.application.clinic.ports.in.GetAdminClinicDetailUseCase;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VeterinaryNotFoundException;
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

@WebMvcTest(controllers = GetAdminClinicDetailController.class)
@Import(GlobalExceptionHandler.class)
@ImportSecurityConfig
class GetAdminClinicDetailControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetAdminClinicDetailUseCase getAdminClinicDetailUseCase;

    @Test
    void getDetail_shouldReturn200_whenCallerIsPlatformAdmin() throws Exception {
        UUID vetId = UUID.randomUUID();
        when(getAdminClinicDetailUseCase.getDetail(any(UUID.class)))
                .thenReturn(AdminClinicDetailResponse.builder().id(vetId).name("Clinica Test").status(VeterinaryStatus.ACTIVE).build());

        mockMvc.perform(get("/api/veterinary/admin/veterinaries/{id}", vetId)
                        .with(csrf())
                        .with(jwt().authorities(new SimpleGrantedAuthority("PLATFORM_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Clinica Test"));
    }

    @Test
    void getDetail_shouldReturn404_whenClinicNotFound() throws Exception {
        UUID vetId = UUID.randomUUID();
        when(getAdminClinicDetailUseCase.getDetail(any(UUID.class))).thenThrow(new VeterinaryNotFoundException());

        mockMvc.perform(get("/api/veterinary/admin/veterinaries/{id}", vetId)
                        .with(csrf())
                        .with(jwt().authorities(new SimpleGrantedAuthority("PLATFORM_ADMIN"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void getDetail_shouldReturn403_whenCallerLacksPlatformAdminAuthority() throws Exception {
        UUID vetId = UUID.randomUUID();
        mockMvc.perform(get("/api/veterinary/admin/veterinaries/{id}", vetId)
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void getDetail_shouldReturn401_whenNoTokenIsPresent() throws Exception {
        UUID vetId = UUID.randomUUID();
        mockMvc.perform(get("/api/veterinary/admin/veterinaries/{id}", vetId)
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}

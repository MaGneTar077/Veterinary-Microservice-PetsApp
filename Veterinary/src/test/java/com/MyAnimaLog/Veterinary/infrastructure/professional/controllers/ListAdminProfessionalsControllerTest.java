package com.MyAnimaLog.Veterinary.infrastructure.professional.controllers;

import com.MyAnimaLog.Veterinary.application.professional.dto.AdminProfessionalListItem;
import com.MyAnimaLog.Veterinary.application.professional.dto.AdminProfessionalListResponse;
import com.MyAnimaLog.Veterinary.application.professional.ports.in.ListAdminProfessionalsUseCase;
import com.MyAnimaLog.Veterinary.domain.professional.enums.LicenseStatus;
import com.MyAnimaLog.Veterinary.infrastructure.config.GlobalExceptionHandler;
import com.MyAnimaLog.Veterinary.infrastructure.security.ImportSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ListAdminProfessionalsController.class)
@Import(GlobalExceptionHandler.class)
@ImportSecurityConfig
class ListAdminProfessionalsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListAdminProfessionalsUseCase listAdminProfessionalsUseCase;

    @Test
    void list_shouldReturn200WithItems_whenCallerIsPlatformAdmin() throws Exception {
        AdminProfessionalListItem item = AdminProfessionalListItem.builder()
                .id(UUID.randomUUID()).fullName("Juan Pérez").licenseStatus(LicenseStatus.VERIFIED).build();
        when(listAdminProfessionalsUseCase.list(any(), any(), any(), any()))
                .thenReturn(AdminProfessionalListResponse.builder().items(List.of(item)).page(0).size(20).total(1).build());

        mockMvc.perform(get("/api/veterinary/admin/professionals")
                        .with(csrf())
                        .with(jwt().authorities(new SimpleGrantedAuthority("PLATFORM_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].fullName").value("Juan Pérez"))
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void list_shouldPassFiltersAndPaginationThrough() throws Exception {
        when(listAdminProfessionalsUseCase.list(eq("juan"), eq(true), eq(1), eq(10)))
                .thenReturn(AdminProfessionalListResponse.builder().items(List.of()).page(1).size(10).total(0).build());

        mockMvc.perform(get("/api/veterinary/admin/professionals")
                        .param("search", "juan")
                        .param("verified", "true")
                        .param("page", "1")
                        .param("size", "10")
                        .with(csrf())
                        .with(jwt().authorities(new SimpleGrantedAuthority("PLATFORM_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(10));
    }

    @Test
    void list_shouldReturn403_whenCallerLacksPlatformAdminAuthority() throws Exception {
        mockMvc.perform(get("/api/veterinary/admin/professionals")
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void list_shouldReturn401_whenNoTokenIsPresent() throws Exception {
        mockMvc.perform(get("/api/veterinary/admin/professionals")
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}

package com.MyAnimaLog.Veterinary.infrastructure.clinic.controllers;

import com.MyAnimaLog.Veterinary.application.clinic.dto.AdminClinicListItem;
import com.MyAnimaLog.Veterinary.application.clinic.dto.AdminClinicListResponse;
import com.MyAnimaLog.Veterinary.application.clinic.ports.in.ListAdminClinicsUseCase;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
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

@WebMvcTest(controllers = ListAdminClinicsController.class)
@Import(GlobalExceptionHandler.class)
@ImportSecurityConfig
class ListAdminClinicsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListAdminClinicsUseCase listAdminClinicsUseCase;

    @Test
    void list_shouldReturn200WithItems_whenCallerIsPlatformAdmin() throws Exception {
        AdminClinicListItem item = AdminClinicListItem.builder()
                .id(UUID.randomUUID()).name("Clinica Test").status(VeterinaryStatus.ACTIVE).employeesCount(3).build();
        when(listAdminClinicsUseCase.list(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(AdminClinicListResponse.builder().items(List.of(item)).page(0).size(20).total(1).build());

        mockMvc.perform(get("/api/veterinary/admin/veterinaries")
                        .with(csrf())
                        .with(jwt().authorities(new SimpleGrantedAuthority("PLATFORM_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].name").value("Clinica Test"))
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void list_shouldPassFiltersAndPaginationThrough() throws Exception {
        when(listAdminClinicsUseCase.list(eq(VeterinaryStatus.SUSPENDED), eq("vet"), eq("PRO"), eq("Bogota"), eq("Bogota D.C."), eq(1), eq(10)))
                .thenReturn(AdminClinicListResponse.builder().items(List.of()).page(1).size(10).total(0).build());

        mockMvc.perform(get("/api/veterinary/admin/veterinaries")
                        .param("status", "SUSPENDED")
                        .param("search", "vet")
                        .param("plan", "PRO")
                        .param("city", "Bogota")
                        .param("department", "Bogota D.C.")
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
        mockMvc.perform(get("/api/veterinary/admin/veterinaries")
                        .with(csrf())
                        .with(jwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void list_shouldReturn401_whenNoTokenIsPresent() throws Exception {
        mockMvc.perform(get("/api/veterinary/admin/veterinaries")
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}

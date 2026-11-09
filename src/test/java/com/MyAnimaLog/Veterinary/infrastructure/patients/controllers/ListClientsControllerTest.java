package com.MyAnimaLog.Veterinary.infrastructure.patients.controllers;

import com.MyAnimaLog.Veterinary.application.patients.dto.ListClientsResponse;
import com.MyAnimaLog.Veterinary.application.patients.ports.in.ListClientsUseCase;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.InsufficientPermissionException;
import com.MyAnimaLog.Veterinary.domain.security.Permission;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ListClientsController.class)
@Import(GlobalExceptionHandler.class)
@ImportSecurityConfig
class ListClientsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListClientsUseCase listClientsUseCase;

    @Test
    void list_shouldReturn200() throws Exception {
        UUID veterinaryId = UUID.randomUUID();
        when(listClientsUseCase.list(any(), any(), any())).thenReturn(ListClientsResponse.builder()
                .items(List.of()).page(0).size(20).total(0).build());

        mockMvc.perform(get("/api/veterinary/{id}/clients", veterinaryId).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void list_shouldReturn403_whenCallerLacksPermission() throws Exception {
        when(listClientsUseCase.list(any(), any(), any()))
                .thenThrow(new InsufficientPermissionException(Permission.CLINICAL_READ_BASIC));

        mockMvc.perform(get("/api/veterinary/{id}/clients", UUID.randomUUID()).with(jwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void list_shouldReturn401_whenNoTokenIsPresent() throws Exception {
        mockMvc.perform(get("/api/veterinary/{id}/clients", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }
}

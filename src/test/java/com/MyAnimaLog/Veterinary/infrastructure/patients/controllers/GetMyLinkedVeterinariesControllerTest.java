package com.MyAnimaLog.Veterinary.infrastructure.patients.controllers;

import com.MyAnimaLog.Veterinary.application.patients.dto.GetMyLinkedVeterinariesResponse;
import com.MyAnimaLog.Veterinary.application.patients.ports.in.GetMyLinkedVeterinariesUseCase;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.infrastructure.config.GlobalExceptionHandler;
import com.MyAnimaLog.Veterinary.infrastructure.security.ImportSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = GetMyLinkedVeterinariesController.class)
@Import(GlobalExceptionHandler.class)
@ImportSecurityConfig
class GetMyLinkedVeterinariesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetMyLinkedVeterinariesUseCase getMyLinkedVeterinariesUseCase;

    @Test
    void list_shouldReturn200_withTheLinkedClinics() throws Exception {
        UUID veterinaryId = UUID.randomUUID();
        when(getMyLinkedVeterinariesUseCase.list()).thenReturn(
                GetMyLinkedVeterinariesResponse.builder()
                        .veterinaries(List.of(GetMyLinkedVeterinariesResponse.LinkedVeterinary.builder()
                                .veterinaryId(veterinaryId)
                                .name("Clínica El Bosque")
                                .city("Cartagena")
                                .status(VeterinaryStatus.ACTIVE)
                                .linkedAt(LocalDateTime.now())
                                .sharedPetsCount(2)
                                .build()))
                        .build());

        mockMvc.perform(get("/api/veterinary/me/linked-veterinaries").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.veterinaries[0].veterinaryId").value(veterinaryId.toString()))
                .andExpect(jsonPath("$.veterinaries[0].name").value("Clínica El Bosque"))
                .andExpect(jsonPath("$.veterinaries[0].sharedPetsCount").value(2))
                .andExpect(jsonPath("$.veterinaries[0].status").value("ACTIVE"));
    }

    @Test
    void list_shouldReturn200_withAnEmptyList_whenThereAreNoLinks() throws Exception {
        when(getMyLinkedVeterinariesUseCase.list()).thenReturn(
                GetMyLinkedVeterinariesResponse.builder().veterinaries(List.of()).build());

        mockMvc.perform(get("/api/veterinary/me/linked-veterinaries").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.veterinaries").isEmpty());
    }

    @Test
    void list_shouldReturn401_whenNoTokenIsPresent() throws Exception {
        mockMvc.perform(get("/api/veterinary/me/linked-veterinaries"))
                .andExpect(status().isUnauthorized());
    }
}

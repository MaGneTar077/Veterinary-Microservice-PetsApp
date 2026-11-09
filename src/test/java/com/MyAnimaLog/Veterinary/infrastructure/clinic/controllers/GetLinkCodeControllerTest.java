package com.MyAnimaLog.Veterinary.infrastructure.clinic.controllers;

import com.MyAnimaLog.Veterinary.application.clinic.dto.GetLinkCodeResponse;
import com.MyAnimaLog.Veterinary.application.clinic.ports.in.GetLinkCodeUseCase;
import com.MyAnimaLog.Veterinary.domain.security.Permission;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.InsufficientPermissionException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.TenantMismatchException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VeterinaryNotFoundException;
import com.MyAnimaLog.Veterinary.infrastructure.config.GlobalExceptionHandler;
import com.MyAnimaLog.Veterinary.infrastructure.security.ImportSecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = GetLinkCodeController.class)
@Import(GlobalExceptionHandler.class)
@ImportSecurityConfig
class GetLinkCodeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetLinkCodeUseCase getLinkCodeUseCase;

    private UUID veterinaryId;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();
    }

    @Test
    void getLinkCode_shouldReturn200_withTheCodeAndLink() throws Exception {
        when(getLinkCodeUseCase.getLinkCode(any(UUID.class))).thenReturn(GetLinkCodeResponse.builder()
                .veterinaryId(veterinaryId)
                .inviteCode("ABCD2345")
                .inviteLink("https://app.myanimalog.com/clinicas/vincular?code=ABCD2345")
                .hasCode(true)
                .build());

        mockMvc.perform(get("/api/veterinary/{veterinaryId}/link-code", veterinaryId).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inviteCode").value("ABCD2345"))
                .andExpect(jsonPath("$.inviteLink")
                        .value("https://app.myanimalog.com/clinicas/vincular?code=ABCD2345"))
                .andExpect(jsonPath("$.hasCode").value(true));
    }

    @Test
    void getLinkCode_shouldReturn200WithNulls_whenClinicHasNoCodeYet() throws Exception {
        when(getLinkCodeUseCase.getLinkCode(any(UUID.class))).thenReturn(GetLinkCodeResponse.builder()
                .veterinaryId(veterinaryId)
                .hasCode(false)
                .build());

        mockMvc.perform(get("/api/veterinary/{veterinaryId}/link-code", veterinaryId).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasCode").value(false))
                .andExpect(jsonPath("$.inviteCode").doesNotExist());
    }

    @Test
    void getLinkCode_shouldReturn403_whenCallerLacksClinicConfigure() throws Exception {
        when(getLinkCodeUseCase.getLinkCode(any(UUID.class)))
                .thenThrow(new InsufficientPermissionException(Permission.CLINIC_CONFIGURE));

        mockMvc.perform(get("/api/veterinary/{veterinaryId}/link-code", veterinaryId).with(jwt()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_PERMISSION"));
    }

    @Test
    void getLinkCode_shouldReturn403_whenCallerBelongsToAnotherClinic() throws Exception {
        when(getLinkCodeUseCase.getLinkCode(any(UUID.class))).thenThrow(new TenantMismatchException());

        mockMvc.perform(get("/api/veterinary/{veterinaryId}/link-code", veterinaryId).with(jwt()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("TENANT_MISMATCH"));
    }

    @Test
    void getLinkCode_shouldReturn404_whenClinicDoesNotExist() throws Exception {
        when(getLinkCodeUseCase.getLinkCode(any(UUID.class))).thenThrow(new VeterinaryNotFoundException());

        mockMvc.perform(get("/api/veterinary/{veterinaryId}/link-code", veterinaryId).with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("VETERINARY_NOT_FOUND"));
    }

    @Test
    void getLinkCode_shouldReturn401_whenNoTokenIsPresent() throws Exception {
        mockMvc.perform(get("/api/veterinary/{veterinaryId}/link-code", veterinaryId))
                .andExpect(status().isUnauthorized());
    }
}

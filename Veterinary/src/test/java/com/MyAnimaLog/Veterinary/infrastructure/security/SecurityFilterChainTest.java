package com.MyAnimaLog.Veterinary.infrastructure.security;

import com.MyAnimaLog.Veterinary.application.clinic.dto.RegisterVeterinaryRequest;
import com.MyAnimaLog.Veterinary.application.clinic.ports.in.RegisterVeterinaryUseCase;
import com.MyAnimaLog.Veterinary.infrastructure.clinic.controllers.RegisterVeterinaryController;
import com.MyAnimaLog.Veterinary.infrastructure.config.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cross-cutting SecurityConfig behavior that isn't tied to one controller's business logic:
 * missing-token handling and the {@code /admin/**} authority gate. Uses
 * {@code RegisterVeterinaryController} only as a stand-in protected endpoint.
 */
@WebMvcTest(controllers = RegisterVeterinaryController.class)
@Import(GlobalExceptionHandler.class)
@ImportSecurityConfig
class SecurityFilterChainTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RegisterVeterinaryUseCase registerVeterinaryUseCase;

    @Test
    void protectedEndpoint_shouldReturn401WithHandlerJsonFormat_whenNoTokenIsPresent() throws Exception {
        RegisterVeterinaryRequest request = RegisterVeterinaryRequest.builder()
                .name("Clinica Sin Token")
                .city("Bogota")
                .email("sintoken@veterinaria.com")
                .build();

        mockMvc.perform(post("/api/veterinary/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void adminEndpoint_shouldReturn403_whenTokenHasNoPlatformAdminAuthority() throws Exception {
        mockMvc.perform(get("/admin/anything").with(jwt()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void adminEndpoint_shouldPassAuthorizationGate_whenTokenHasPlatformAdminAuthority() throws Exception {
        // No /admin/** controller exists yet (comes in a later task), so there is no handler to
        // route to. GlobalExceptionHandler's catch-all `Exception.class` handler (pre-existing,
        // out of scope here) turns Spring's "no handler found" into a 500 instead of a 404 — so
        // we only assert that the authorization gate itself did not block the request (no 403),
        // rather than asserting a specific downstream status.
        mockMvc.perform(post("/admin/anything")
                        .with(jwt().authorities(new SimpleGrantedAuthority("PLATFORM_ADMIN"))))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(403));
    }
}

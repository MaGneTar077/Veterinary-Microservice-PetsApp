package com.MyAnimaLog.Veterinary.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InternalApiKeyFilterTest {

    private static final String VALID_KEY = "test-internal-key";

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;

    private final InternalApiKeyFilter filter =
            new InternalApiKeyFilter(VALID_KEY, new RestAuthenticationEntryPoint());

    @BeforeEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilter_passesThrough_whenPathIsNotInternal() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/veterinary/register");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilter_authenticatesAndContinues_whenKeyMatches() throws Exception {
        when(request.getRequestURI()).thenReturn("/internal/veterinaries/x/members/y");
        when(request.getHeader("X-Internal-Api-Key")).thenReturn(VALID_KEY);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting(Object::toString)
                .contains("INTERNAL_SERVICE");
    }

    @Test
    void doFilter_rejectsWith401_whenKeyIsMissing() throws Exception {
        when(request.getRequestURI()).thenReturn("/internal/veterinaries/x/members/y");
        when(request.getHeader("X-Internal-Api-Key")).thenReturn(null);
        when(response.getOutputStream()).thenReturn(new DummyServletOutputStream());

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        verify(response).setStatus(401);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilter_rejectsWith401_whenKeyIsWrong() throws Exception {
        when(request.getRequestURI()).thenReturn("/internal/veterinaries/x/members/y");
        when(request.getHeader("X-Internal-Api-Key")).thenReturn("wrong-key");
        when(response.getOutputStream()).thenReturn(new DummyServletOutputStream());

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        verify(response).setStatus(401);
    }

    private static class DummyServletOutputStream extends jakarta.servlet.ServletOutputStream {
        @Override
        public void write(int b) {
            // discard
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setWriteListener(jakarta.servlet.WriteListener writeListener) {
            // not needed for this test
        }
    }
}

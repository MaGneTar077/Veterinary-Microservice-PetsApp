package com.MyAnimaLog.Veterinary.application.verification.services;

import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.application.verification.dto.RerunVerificationAnalysisResponse;
import com.MyAnimaLog.Veterinary.application.verification.dto.VerificationSubmittedEvent;
import com.MyAnimaLog.Veterinary.application.verification.ports.out.IaVerificationPort;
import com.MyAnimaLog.Veterinary.application.verification.ports.out.VerificationRequestRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.PlatformAdminRequiredException;
import com.MyAnimaLog.Veterinary.domain.verification.enums.AiStatus;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.AiVerificationDisabledException;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.VerificationRequestNotFoundException;
import com.MyAnimaLog.Veterinary.domain.verification.model.VerificationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RerunVerificationAnalysisServiceTest {

    @Mock
    private VeterinaryAuthorizationService authorizationService;
    @Mock
    private VerificationRequestRepositoryPort verificationRequestRepositoryPort;
    @Mock
    private IaVerificationPort iaVerificationPort;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private RerunVerificationAnalysisService service;

    private UUID requestId;

    @BeforeEach
    void setUp() {
        requestId = UUID.randomUUID();
    }

    @Test
    void rerun_shouldPublishEvent_whenAiEnabled() {
        when(iaVerificationPort.isEnabled()).thenReturn(true);
        when(verificationRequestRepositoryPort.findById(requestId)).thenReturn(Optional.of(
                VerificationRequest.builder().id(requestId).aiStatus(AiStatus.DONE).build()));

        RerunVerificationAnalysisResponse response = service.rerun(requestId);

        assertThat(response.getRequestId()).isEqualTo(requestId);
        verify(applicationEventPublisher).publishEvent(new VerificationSubmittedEvent(requestId));
    }

    @Test
    void rerun_shouldThrowAiVerificationDisabled_whenAiOff() {
        when(iaVerificationPort.isEnabled()).thenReturn(false);

        assertThatThrownBy(() -> service.rerun(requestId))
                .isInstanceOf(AiVerificationDisabledException.class);

        verify(applicationEventPublisher, never()).publishEvent(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rerun_shouldThrowVerificationRequestNotFound_whenMissing() {
        when(iaVerificationPort.isEnabled()).thenReturn(true);
        when(verificationRequestRepositoryPort.findById(requestId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.rerun(requestId))
                .isInstanceOf(VerificationRequestNotFoundException.class);
    }

    @Test
    void rerun_shouldRequirePlatformAdmin() {
        doThrow(new PlatformAdminRequiredException()).when(authorizationService).requirePlatformAdmin();

        assertThatThrownBy(() -> service.rerun(requestId))
                .isInstanceOf(PlatformAdminRequiredException.class);
    }
}

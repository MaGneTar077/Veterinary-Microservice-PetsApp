package com.MyAnimaLog.Veterinary.application.verification.services;

import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.dto.AuthenticatedUser;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.AuthenticatedUserPort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.application.verification.dto.RetryClinicVerificationResponse;
import com.MyAnimaLog.Veterinary.application.verification.ports.out.VerificationRequestRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.NotVeterinaryOwnerException;
import com.MyAnimaLog.Veterinary.domain.verification.enums.SubjectType;
import com.MyAnimaLog.Veterinary.domain.verification.enums.VerificationRequestStatus;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.InvalidVerificationStateException;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.VerificationAttemptsExhaustedException;
import com.MyAnimaLog.Veterinary.domain.verification.model.VerificationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RetryClinicVerificationServiceTest {

    @Mock
    private VeterinaryRepositoryPort veterinaryRepositoryPort;

    @Mock
    private VerificationRequestRepositoryPort verificationRequestRepositoryPort;

    @Mock
    private AuthenticatedUserPort authenticatedUserPort;

    @Mock
    private VeterinaryAuthorizationService authorizationService;

    @InjectMocks
    private RetryClinicVerificationService service;

    private UUID veterinaryId;
    private UUID callerId;
    private Veterinary veterinary;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();
        callerId = UUID.randomUUID();
        veterinary = Veterinary.builder().id(veterinaryId).build();

        lenient().when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(veterinary));
        lenient().when(veterinaryRepositoryPort.save(any(Veterinary.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(verificationRequestRepositoryPort.save(any(VerificationRequest.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(authenticatedUserPort.current()).thenReturn(
                new AuthenticatedUser(callerId, "owner@example.com", true, false, Optional.empty()));
    }

    private VerificationRequest rejectedRequest(int attemptNumber) {
        return VerificationRequest.builder().id(UUID.randomUUID())
                .status(VerificationRequestStatus.REJECTED).attemptNumber(attemptNumber).build();
    }

    @Test
    void retry_shouldCreateNewDraftRequest_withIncrementedAttemptNumber() {
        when(verificationRequestRepositoryPort.findLatestBySubject(SubjectType.CLINIC, veterinaryId))
                .thenReturn(Optional.of(rejectedRequest(1)));

        RetryClinicVerificationResponse response = service.retry(veterinaryId);

        assertThat(response.getAttemptNumber()).isEqualTo(2);
        assertThat(response.getStatus()).isEqualTo(VerificationRequestStatus.DRAFT);
        verify(veterinaryRepositoryPort).save(argThat((Veterinary v) -> v.getStatus() == VeterinaryStatus.PENDING_DOCUMENTS));
    }

    @Test
    void retry_shouldAllowThirdAttempt_whenSecondWasRejected() {
        when(verificationRequestRepositoryPort.findLatestBySubject(SubjectType.CLINIC, veterinaryId))
                .thenReturn(Optional.of(rejectedRequest(2)));

        RetryClinicVerificationResponse response = service.retry(veterinaryId);

        assertThat(response.getAttemptNumber()).isEqualTo(3);
    }

    @Test
    void retry_shouldThrowAttemptsExhausted_whenThirdAttemptWasAlsoRejected() {
        when(verificationRequestRepositoryPort.findLatestBySubject(SubjectType.CLINIC, veterinaryId))
                .thenReturn(Optional.of(rejectedRequest(3)));

        assertThatThrownBy(() -> service.retry(veterinaryId))
                .isInstanceOf(VerificationAttemptsExhaustedException.class);

        verify(verificationRequestRepositoryPort, never()).save(any());
    }

    @Test
    void retry_shouldThrowInvalidVerificationState_whenLatestRequestIsNotRejected() {
        when(verificationRequestRepositoryPort.findLatestBySubject(SubjectType.CLINIC, veterinaryId))
                .thenReturn(Optional.of(VerificationRequest.builder().id(UUID.randomUUID())
                        .status(VerificationRequestStatus.DRAFT).attemptNumber(1).build()));

        assertThatThrownBy(() -> service.retry(veterinaryId))
                .isInstanceOf(InvalidVerificationStateException.class);
    }

    @Test
    void retry_shouldPropagateNotVeterinaryOwner() {
        doThrow(new NotVeterinaryOwnerException()).when(authorizationService).requireVeterinaryOwner(veterinaryId);

        assertThatThrownBy(() -> service.retry(veterinaryId))
                .isInstanceOf(NotVeterinaryOwnerException.class);
    }
}

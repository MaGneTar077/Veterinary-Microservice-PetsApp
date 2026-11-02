package com.MyAnimaLog.Veterinary.application.clinic.services;

import com.MyAnimaLog.Veterinary.application.clinic.dto.UpdateLegalRepresentativeRequest;
import com.MyAnimaLog.Veterinary.application.clinic.dto.UpdateLegalRepresentativeResponse;
import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.application.verification.ports.out.VerificationRequestRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.NotVeterinaryOwnerException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VerificationLockedException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VeterinaryNotFoundException;
import com.MyAnimaLog.Veterinary.domain.verification.enums.SubjectType;
import com.MyAnimaLog.Veterinary.domain.verification.enums.VerificationRequestStatus;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.InvalidLegalRepresentativeException;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateLegalRepresentativeServiceTest {

    @Mock
    private VeterinaryRepositoryPort veterinaryRepositoryPort;

    @Mock
    private VerificationRequestRepositoryPort verificationRequestRepositoryPort;

    @Mock
    private VeterinaryAuthorizationService authorizationService;

    @InjectMocks
    private UpdateLegalRepresentativeService service;

    private UUID veterinaryId;
    private Veterinary veterinary;
    private UpdateLegalRepresentativeRequest validRequest;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();
        veterinary = Veterinary.builder().id(veterinaryId).build();
        validRequest = UpdateLegalRepresentativeRequest.builder()
                .isOwner(true)
                .fullName("Juan Pérez")
                .documentType("CC")
                .documentNumber("123456789")
                .build();

        lenient().when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(veterinary));
        lenient().when(veterinaryRepositoryPort.save(any(Veterinary.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(verificationRequestRepositoryPort.findLatestBySubject(SubjectType.CLINIC, veterinaryId))
                .thenReturn(Optional.of(openRequest()));
    }

    private VerificationRequest openRequest() {
        return VerificationRequest.builder().id(UUID.randomUUID()).status(VerificationRequestStatus.DRAFT).build();
    }

    @Test
    void update_shouldSucceed_whenCallerIsOwnerAndRequestIsDraft() {
        UpdateLegalRepresentativeResponse response = service.updateLegalRepresentative(veterinaryId, validRequest);

        assertThat(response.getFullName()).isEqualTo("Juan Pérez");
        assertThat(response.getDocumentType()).isEqualTo("CC");
        assertThat(response.getIsOwner()).isTrue();
    }

    @Test
    void update_shouldSucceed_whenRequestIsNeedsCorrection() {
        when(verificationRequestRepositoryPort.findLatestBySubject(SubjectType.CLINIC, veterinaryId))
                .thenReturn(Optional.of(VerificationRequest.builder().id(UUID.randomUUID())
                        .status(VerificationRequestStatus.NEEDS_CORRECTION).build()));

        assertThat(service.updateLegalRepresentative(veterinaryId, validRequest)).isNotNull();
    }

    @Test
    void update_shouldThrowNotVeterinaryOwner_whenCallerIsNotOwner() {
        doThrow(new NotVeterinaryOwnerException()).when(authorizationService).requireVeterinaryOwner(veterinaryId);

        assertThatThrownBy(() -> service.updateLegalRepresentative(veterinaryId, validRequest))
                .isInstanceOf(NotVeterinaryOwnerException.class);
    }

    @Test
    void update_shouldThrowVerificationLocked_whenRequestIsUnderReview() {
        when(verificationRequestRepositoryPort.findLatestBySubject(SubjectType.CLINIC, veterinaryId))
                .thenReturn(Optional.of(VerificationRequest.builder().id(UUID.randomUUID())
                        .status(VerificationRequestStatus.UNDER_REVIEW).build()));

        assertThatThrownBy(() -> service.updateLegalRepresentative(veterinaryId, validRequest))
                .isInstanceOf(VerificationLockedException.class);
    }

    @Test
    void update_shouldThrowVerificationLocked_whenNoVerificationRequestExists() {
        when(verificationRequestRepositoryPort.findLatestBySubject(SubjectType.CLINIC, veterinaryId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateLegalRepresentative(veterinaryId, validRequest))
                .isInstanceOf(VerificationLockedException.class);
    }

    @Test
    void update_shouldThrowVeterinaryNotFound_whenVeterinaryDoesNotExist() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateLegalRepresentative(veterinaryId, validRequest))
                .isInstanceOf(VeterinaryNotFoundException.class);
    }

    @Test
    void update_shouldThrowInvalidLegalRepresentative_whenIsOwnerIsNull() {
        validRequest.setIsOwner(null);

        assertThatThrownBy(() -> service.updateLegalRepresentative(veterinaryId, validRequest))
                .isInstanceOf(InvalidLegalRepresentativeException.class);
    }

    @Test
    void update_shouldThrowInvalidLegalRepresentative_whenDocumentTypeIsInvalid() {
        validRequest.setDocumentType("TI");

        assertThatThrownBy(() -> service.updateLegalRepresentative(veterinaryId, validRequest))
                .isInstanceOf(InvalidLegalRepresentativeException.class);
    }

    @Test
    void update_shouldThrowInvalidLegalRepresentative_whenCcDocumentNumberHasLetters() {
        validRequest.setDocumentNumber("123ABC");

        assertThatThrownBy(() -> service.updateLegalRepresentative(veterinaryId, validRequest))
                .isInstanceOf(InvalidLegalRepresentativeException.class);
    }

    @Test
    void update_shouldAcceptAlphanumericDocumentNumber_forCe() {
        validRequest.setDocumentType("CE");
        validRequest.setDocumentNumber("AB123456");

        assertThat(service.updateLegalRepresentative(veterinaryId, validRequest)).isNotNull();
    }

    @Test
    void update_shouldThrowInvalidLegalRepresentative_whenFullNameIsBlank() {
        validRequest.setFullName("  ");

        assertThatThrownBy(() -> service.updateLegalRepresentative(veterinaryId, validRequest))
                .isInstanceOf(InvalidLegalRepresentativeException.class);
    }
}

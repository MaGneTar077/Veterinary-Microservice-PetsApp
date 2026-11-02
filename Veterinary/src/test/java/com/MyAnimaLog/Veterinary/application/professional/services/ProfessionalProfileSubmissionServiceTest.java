package com.MyAnimaLog.Veterinary.application.professional.services;

import com.MyAnimaLog.Veterinary.application.professional.dto.SubmitProfessionalProfileResponse;
import com.MyAnimaLog.Veterinary.application.professional.ports.out.ProfessionalProfileRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.EventPublisherPort;
import com.MyAnimaLog.Veterinary.application.verification.ports.out.VerificationDocumentRepositoryPort;
import com.MyAnimaLog.Veterinary.application.verification.ports.out.VerificationRequestRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.professional.enums.LicenseStatus;
import com.MyAnimaLog.Veterinary.domain.professional.exceptions.ProfessionalProfileNotFoundException;
import com.MyAnimaLog.Veterinary.domain.professional.model.ProfessionalProfile;
import com.MyAnimaLog.Veterinary.domain.verification.enums.SubjectType;
import com.MyAnimaLog.Veterinary.domain.verification.enums.VerificationRequestStatus;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.InvalidVerificationStateException;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.MissingRequiredDocumentsException;
import com.MyAnimaLog.Veterinary.domain.verification.model.VerificationDocument;
import com.MyAnimaLog.Veterinary.domain.verification.model.VerificationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfessionalProfileSubmissionServiceTest {

    @Mock
    private ProfessionalProfileRepositoryPort professionalProfileRepositoryPort;

    @Mock
    private VerificationRequestRepositoryPort verificationRequestRepositoryPort;

    @Mock
    private VerificationDocumentRepositoryPort verificationDocumentRepositoryPort;

    @Mock
    private EventPublisherPort eventPublisherPort;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private ProfessionalProfileSubmissionService service;

    private UUID userId;
    private ProfessionalProfile profile;
    private VerificationRequest draftRequest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "supportEmail", "support@myanimalog.com");

        userId = UUID.randomUUID();
        profile = ProfessionalProfile.builder().id(UUID.randomUUID()).userId(userId)
                .licenseStatus(LicenseStatus.NOT_SUBMITTED).build();
        draftRequest = VerificationRequest.builder().id(UUID.randomUUID()).status(VerificationRequestStatus.DRAFT).build();

        lenient().when(professionalProfileRepositoryPort.findByUserId(userId)).thenReturn(Optional.of(profile));
        lenient().when(professionalProfileRepositoryPort.save(any(ProfessionalProfile.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(verificationRequestRepositoryPort.findLatestBySubject(SubjectType.PROFESSIONAL, profile.getId()))
                .thenReturn(Optional.of(draftRequest));
        lenient().when(verificationRequestRepositoryPort.save(any(VerificationRequest.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(verificationDocumentRepositoryPort.findByRequestId(draftRequest.getId()))
                .thenReturn(List.of(doc("PROFESSIONAL_CARD"), doc("ID_DOCUMENT")));
    }

    private VerificationDocument doc(String type) {
        return VerificationDocument.builder().id(UUID.randomUUID()).requestId(draftRequest.getId()).documentType(type).build();
    }

    @Test
    void submitForUser_shouldSucceed_whenRequiredDocumentsPresent() {
        SubmitProfessionalProfileResponse response = service.submitForUser(userId);

        assertThat(response.getStatus()).isEqualTo(VerificationRequestStatus.UNDER_REVIEW);
        assertThat(response.getLicenseStatus()).isEqualTo(LicenseStatus.PENDING);
        verify(eventPublisherPort, times(1)).publish(any());
    }

    @Test
    void submitForUser_shouldThrowMissingRequiredDocuments_whenCardIsMissing() {
        when(verificationDocumentRepositoryPort.findByRequestId(draftRequest.getId()))
                .thenReturn(List.of(doc("ID_DOCUMENT")));

        assertThatThrownBy(() -> service.submitForUser(userId))
                .isInstanceOf(MissingRequiredDocumentsException.class);

        verify(professionalProfileRepositoryPort, never()).save(any());
        verify(eventPublisherPort, never()).publish(any());
    }

    @Test
    void submitForUser_shouldThrowProfessionalProfileNotFound_whenNoProfile() {
        when(professionalProfileRepositoryPort.findByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.submitForUser(userId))
                .isInstanceOf(ProfessionalProfileNotFoundException.class);
    }

    @Test
    void submitForUser_shouldThrowInvalidVerificationState_whenRequestIsUnderReview() {
        when(verificationRequestRepositoryPort.findLatestBySubject(SubjectType.PROFESSIONAL, profile.getId()))
                .thenReturn(Optional.of(VerificationRequest.builder().id(UUID.randomUUID())
                        .status(VerificationRequestStatus.UNDER_REVIEW).build()));

        assertThatThrownBy(() -> service.submitForUser(userId))
                .isInstanceOf(InvalidVerificationStateException.class);
    }
}

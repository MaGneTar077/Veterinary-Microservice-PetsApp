package com.MyAnimaLog.Veterinary.application.verification.services;

import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.professional.ports.out.ProfessionalProfileRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.dto.AuthenticatedUser;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.AuthenticatedUserPort;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.EventPublisherPort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.application.subscription.ports.out.VeterinarySubscriptionRepositoryPort;
import com.MyAnimaLog.Veterinary.application.verification.dto.ApproveVerificationRequest;
import com.MyAnimaLog.Veterinary.application.verification.dto.ApproveVerificationResponse;
import com.MyAnimaLog.Veterinary.application.verification.ports.out.VerificationRequestRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.professional.enums.LicenseStatus;
import com.MyAnimaLog.Veterinary.domain.professional.model.ProfessionalProfile;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.PlatformAdminRequiredException;
import com.MyAnimaLog.Veterinary.domain.subscription.model.VeterinarySubscription;
import com.MyAnimaLog.Veterinary.domain.verification.enums.SubjectType;
import com.MyAnimaLog.Veterinary.domain.verification.enums.VerificationRequestStatus;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.CannotReviewOwnRequestException;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.InvalidVerificationStateException;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.RegistryCheckRequiredException;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApproveVerificationServiceTest {

    @Mock
    private VeterinaryAuthorizationService authorizationService;
    @Mock
    private AuthenticatedUserPort authenticatedUserPort;
    @Mock
    private VerificationRequestRepositoryPort verificationRequestRepositoryPort;
    @Mock
    private VeterinaryRepositoryPort veterinaryRepositoryPort;
    @Mock
    private ProfessionalProfileRepositoryPort professionalProfileRepositoryPort;
    @Mock
    private VeterinarySubscriptionRepositoryPort subscriptionRepositoryPort;
    @Mock
    private EventPublisherPort eventPublisherPort;

    @InjectMocks
    private ApproveVerificationService service;

    private UUID requestId;
    private UUID submittedBy;
    private UUID adminId;

    @BeforeEach
    void setUp() {
        requestId = UUID.randomUUID();
        submittedBy = UUID.randomUUID();
        adminId = UUID.randomUUID();
        lenient().when(authenticatedUserPort.current()).thenReturn(
                new AuthenticatedUser(adminId, "admin@x.com", true, true, Optional.empty()));
        lenient().when(verificationRequestRepositoryPort.save(any(VerificationRequest.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(verificationRequestRepositoryPort.claimForDecision(
                        requestId, VerificationRequestStatus.UNDER_REVIEW, VerificationRequestStatus.APPROVED))
                .thenReturn(true);
    }

    @Test
    void approve_shouldActivateClinicAndStartTrial_whenNoActiveSubscriptionExists() {
        UUID veterinaryId = UUID.randomUUID();
        VerificationRequest request = VerificationRequest.builder()
                .id(requestId).subjectType(SubjectType.CLINIC).subjectId(veterinaryId)
                .status(VerificationRequestStatus.UNDER_REVIEW).attemptNumber(1).submittedBy(submittedBy).build();
        when(verificationRequestRepositoryPort.findById(requestId)).thenReturn(Optional.of(request));
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(
                Veterinary.builder().id(veterinaryId).name("Clinica Test").status(VeterinaryStatus.UNDER_REVIEW).build()));
        when(subscriptionRepositoryPort.existsActiveByVeterinaryId(veterinaryId)).thenReturn(false);

        ApproveVerificationResponse response = service.approve(requestId,
                ApproveVerificationRequest.builder().registryChecked(true).build());

        assertThat(response.getStatus()).isEqualTo(VerificationRequestStatus.APPROVED);
        assertThat(response.getTrialEndsAt()).isNotNull();
        verify(veterinaryRepositoryPort).save(argThatStatusIs(VeterinaryStatus.ACTIVE));
        verify(subscriptionRepositoryPort).save(any(VeterinarySubscription.class));
        verify(eventPublisherPort, org.mockito.Mockito.atLeast(2)).publish(any());
    }

    @Test
    void approve_shouldNotCreateTrial_whenActiveSubscriptionAlreadyExists() {
        UUID veterinaryId = UUID.randomUUID();
        VerificationRequest request = VerificationRequest.builder()
                .id(requestId).subjectType(SubjectType.CLINIC).subjectId(veterinaryId)
                .status(VerificationRequestStatus.UNDER_REVIEW).attemptNumber(1).submittedBy(submittedBy).build();
        when(verificationRequestRepositoryPort.findById(requestId)).thenReturn(Optional.of(request));
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(
                Veterinary.builder().id(veterinaryId).name("Clinica Test").status(VeterinaryStatus.UNDER_REVIEW).build()));
        when(subscriptionRepositoryPort.existsActiveByVeterinaryId(veterinaryId)).thenReturn(true);

        ApproveVerificationResponse response = service.approve(requestId,
                ApproveVerificationRequest.builder().registryChecked(true).build());

        assertThat(response.getTrialEndsAt()).isNull();
        verify(subscriptionRepositoryPort, never()).save(any());
    }

    @Test
    void approve_shouldVerifyProfessionalProfile() {
        UUID profileId = UUID.randomUUID();
        VerificationRequest request = VerificationRequest.builder()
                .id(requestId).subjectType(SubjectType.PROFESSIONAL).subjectId(profileId)
                .status(VerificationRequestStatus.UNDER_REVIEW).attemptNumber(1).submittedBy(submittedBy).build();
        when(verificationRequestRepositoryPort.findById(requestId)).thenReturn(Optional.of(request));
        when(professionalProfileRepositoryPort.findById(profileId)).thenReturn(Optional.of(
                ProfessionalProfile.builder().id(profileId).fullName("Juan Pérez")
                        .licenseStatus(LicenseStatus.PENDING).rejectionReason("old reason").build()));
        when(professionalProfileRepositoryPort.save(any(ProfessionalProfile.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        service.approve(requestId, ApproveVerificationRequest.builder().registryChecked(true).build());

        verify(professionalProfileRepositoryPort).save(org.mockito.ArgumentMatchers.argThat(p ->
                p.getLicenseStatus() == LicenseStatus.VERIFIED && p.getVerifiedBy().equals(adminId) && p.getRejectionReason() == null));
    }

    @Test
    void approve_shouldThrowRegistryCheckRequired_whenNotTrue() {
        assertThatThrownBy(() -> service.approve(requestId, ApproveVerificationRequest.builder().registryChecked(false).build()))
                .isInstanceOf(RegistryCheckRequiredException.class);
    }

    @Test
    void approve_shouldThrowInvalidState_whenNotUnderReview() {
        VerificationRequest request = VerificationRequest.builder()
                .id(requestId).subjectType(SubjectType.CLINIC).subjectId(UUID.randomUUID())
                .status(VerificationRequestStatus.DRAFT).attemptNumber(1).submittedBy(submittedBy).build();
        when(verificationRequestRepositoryPort.findById(requestId)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.approve(requestId, ApproveVerificationRequest.builder().registryChecked(true).build()))
                .isInstanceOf(InvalidVerificationStateException.class);
    }

    @Test
    void approve_shouldThrowCannotReviewOwnRequest_whenAdminSubmittedIt() {
        VerificationRequest request = VerificationRequest.builder()
                .id(requestId).subjectType(SubjectType.CLINIC).subjectId(UUID.randomUUID())
                .status(VerificationRequestStatus.UNDER_REVIEW).attemptNumber(1).submittedBy(adminId).build();
        when(verificationRequestRepositoryPort.findById(requestId)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.approve(requestId, ApproveVerificationRequest.builder().registryChecked(true).build()))
                .isInstanceOf(CannotReviewOwnRequestException.class);
    }

    @Test
    void approve_shouldThrowInvalidState_whenAnotherAdminAlreadyDecided() {
        VerificationRequest request = VerificationRequest.builder()
                .id(requestId).subjectType(SubjectType.CLINIC).subjectId(UUID.randomUUID())
                .status(VerificationRequestStatus.UNDER_REVIEW).attemptNumber(1).submittedBy(submittedBy).build();
        when(verificationRequestRepositoryPort.findById(requestId)).thenReturn(Optional.of(request));
        when(verificationRequestRepositoryPort.claimForDecision(
                requestId, VerificationRequestStatus.UNDER_REVIEW, VerificationRequestStatus.APPROVED)).thenReturn(false);

        assertThatThrownBy(() -> service.approve(requestId, ApproveVerificationRequest.builder().registryChecked(true).build()))
                .isInstanceOf(InvalidVerificationStateException.class);

        verify(veterinaryRepositoryPort, never()).save(any());
    }

    @Test
    void approve_shouldRequirePlatformAdmin() {
        doThrow(new PlatformAdminRequiredException()).when(authorizationService).requirePlatformAdmin();

        assertThatThrownBy(() -> service.approve(requestId, ApproveVerificationRequest.builder().registryChecked(true).build()))
                .isInstanceOf(PlatformAdminRequiredException.class);
    }

    private Veterinary argThatStatusIs(VeterinaryStatus status) {
        return org.mockito.ArgumentMatchers.argThat(v -> v.getStatus() == status);
    }
}

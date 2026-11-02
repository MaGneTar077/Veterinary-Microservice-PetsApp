package com.MyAnimaLog.Veterinary.application.professional.services;

import com.MyAnimaLog.Veterinary.application.professional.dto.ProfessionalProfileResponse;
import com.MyAnimaLog.Veterinary.application.professional.dto.UpsertProfessionalProfileRequest;
import com.MyAnimaLog.Veterinary.application.professional.ports.out.ProfessionalProfileRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.dto.AuthenticatedUser;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.AuthenticatedUserPort;
import com.MyAnimaLog.Veterinary.application.verification.ports.out.VerificationRequestRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.professional.enums.IdDocumentType;
import com.MyAnimaLog.Veterinary.domain.professional.enums.LicenseStatus;
import com.MyAnimaLog.Veterinary.domain.professional.enums.Profession;
import com.MyAnimaLog.Veterinary.domain.professional.exceptions.InvalidProfessionalProfileException;
import com.MyAnimaLog.Veterinary.domain.professional.exceptions.LicenseNumberAlreadyExistsException;
import com.MyAnimaLog.Veterinary.domain.professional.model.ProfessionalProfile;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VerificationLockedException;
import com.MyAnimaLog.Veterinary.domain.verification.enums.SubjectType;
import com.MyAnimaLog.Veterinary.domain.verification.enums.VerificationRequestStatus;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.VerificationAttemptsExhaustedException;
import com.MyAnimaLog.Veterinary.domain.verification.model.VerificationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpsertProfessionalProfileServiceTest {

    @Mock
    private ProfessionalProfileRepositoryPort professionalProfileRepositoryPort;

    @Mock
    private VerificationRequestRepositoryPort verificationRequestRepositoryPort;

    @Mock
    private AuthenticatedUserPort authenticatedUserPort;

    @InjectMocks
    private UpsertProfessionalProfileService service;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        lenient().when(authenticatedUserPort.current()).thenReturn(
                new AuthenticatedUser(userId, "vet@example.com", true, false, Optional.empty()));
        lenient().when(professionalProfileRepositoryPort.save(any(ProfessionalProfile.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(verificationRequestRepositoryPort.save(any(VerificationRequest.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    private UpsertProfessionalProfileRequest validRequest() {
        return UpsertProfessionalProfileRequest.builder()
                .fullName("Juan Pérez")
                .documentType("CC")
                .documentNumber("123456789")
                .profession("MV")
                .licenseNumber("LIC-001")
                .specialty("Cirugía")
                .university("U. Nacional")
                .build();
    }

    @Test
    void upsert_shouldCreateProfileAndDraftRequest_whenNoneExists() {
        when(professionalProfileRepositoryPort.findByUserId(userId)).thenReturn(Optional.empty());
        when(professionalProfileRepositoryPort.existsByLicenseNumber("LIC-001")).thenReturn(false);

        ProfessionalProfileResponse response = service.upsert(validRequest());

        assertThat(response.getFullName()).isEqualTo("Juan Pérez");
        assertThat(response.getLicenseStatus()).isEqualTo(LicenseStatus.NOT_SUBMITTED);

        ArgumentCaptor<VerificationRequest> captor = ArgumentCaptor.forClass(VerificationRequest.class);
        verify(verificationRequestRepositoryPort).save(captor.capture());
        assertThat(captor.getValue().getSubjectType()).isEqualTo(SubjectType.PROFESSIONAL);
        assertThat(captor.getValue().getStatus()).isEqualTo(VerificationRequestStatus.DRAFT);
    }

    @Test
    void upsert_shouldThrowLicenseNumberAlreadyExists_onCreate_whenTaken() {
        when(professionalProfileRepositoryPort.findByUserId(userId)).thenReturn(Optional.empty());
        when(professionalProfileRepositoryPort.existsByLicenseNumber("LIC-001")).thenReturn(true);

        assertThatThrownBy(() -> service.upsert(validRequest()))
                .isInstanceOf(LicenseNumberAlreadyExistsException.class);

        verify(professionalProfileRepositoryPort, never()).save(any());
    }

    @Test
    void upsert_shouldThrowInvalidProfessionalProfile_whenFullNameIsBlank() {
        UpsertProfessionalProfileRequest request = validRequest();
        request.setFullName("  ");

        assertThatThrownBy(() -> service.upsert(request))
                .isInstanceOf(InvalidProfessionalProfileException.class);
    }

    @Test
    void upsert_shouldThrowInvalidProfessionalProfile_whenProfessionIsInvalid() {
        UpsertProfessionalProfileRequest request = validRequest();
        request.setProfession("MD");

        assertThatThrownBy(() -> service.upsert(request))
                .isInstanceOf(InvalidProfessionalProfileException.class);
    }

    private ProfessionalProfile existingProfile(LicenseStatus status) {
        return ProfessionalProfile.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .fullName("Juan Pérez")
                .documentType(IdDocumentType.CC)
                .documentNumber("123456789")
                .profession(Profession.MV)
                .licenseNumber("LIC-001")
                .licenseStatus(status)
                .build();
    }

    @Test
    void upsert_shouldThrowVerificationLocked_whenProfileIsPending() {
        when(professionalProfileRepositoryPort.findByUserId(userId)).thenReturn(Optional.of(existingProfile(LicenseStatus.PENDING)));

        assertThatThrownBy(() -> service.upsert(validRequest()))
                .isInstanceOf(VerificationLockedException.class);
    }

    @Test
    void upsert_shouldUpdateFreely_whenNotSubmitted_andOnlyEditingOptionalFields() {
        ProfessionalProfile existing = existingProfile(LicenseStatus.NOT_SUBMITTED);
        when(professionalProfileRepositoryPort.findByUserId(userId)).thenReturn(Optional.of(existing));
        when(professionalProfileRepositoryPort.existsByLicenseNumberAndUserIdNot("LIC-001", userId)).thenReturn(false);

        UpsertProfessionalProfileRequest request = validRequest();
        request.setSpecialty("Dermatología");

        ProfessionalProfileResponse response = service.upsert(request);

        assertThat(response.getSpecialty()).isEqualTo("Dermatología");
        assertThat(response.getLicenseStatus()).isEqualTo(LicenseStatus.NOT_SUBMITTED);
        verify(verificationRequestRepositoryPort, never()).save(any());
    }

    @Test
    void upsert_shouldRevertToNotSubmitted_andCreateNewRequest_whenVerifiedAndIdentityChanges() {
        ProfessionalProfile existing = existingProfile(LicenseStatus.VERIFIED);
        when(professionalProfileRepositoryPort.findByUserId(userId)).thenReturn(Optional.of(existing));
        when(professionalProfileRepositoryPort.existsByLicenseNumberAndUserIdNot(any(), eq(userId))).thenReturn(false);
        when(verificationRequestRepositoryPort.findLatestBySubject(SubjectType.PROFESSIONAL, existing.getId()))
                .thenReturn(Optional.of(VerificationRequest.builder().id(UUID.randomUUID())
                        .status(VerificationRequestStatus.APPROVED).attemptNumber(1).build()));

        UpsertProfessionalProfileRequest request = validRequest();
        request.setFullName("Juan Pérez Cambiado");

        ProfessionalProfileResponse response = service.upsert(request);

        assertThat(response.getLicenseStatus()).isEqualTo(LicenseStatus.NOT_SUBMITTED);
        ArgumentCaptor<VerificationRequest> captor = ArgumentCaptor.forClass(VerificationRequest.class);
        verify(verificationRequestRepositoryPort).save(captor.capture());
        assertThat(captor.getValue().getAttemptNumber()).isEqualTo(2);
        assertThat(captor.getValue().getStatus()).isEqualTo(VerificationRequestStatus.DRAFT);
    }

    @Test
    void upsert_shouldNotCreateNewRequest_whenVerifiedButOnlyOptionalFieldsChange() {
        ProfessionalProfile existing = existingProfile(LicenseStatus.VERIFIED);
        when(professionalProfileRepositoryPort.findByUserId(userId)).thenReturn(Optional.of(existing));
        when(professionalProfileRepositoryPort.existsByLicenseNumberAndUserIdNot(any(), eq(userId))).thenReturn(false);

        UpsertProfessionalProfileRequest request = validRequest();
        request.setUniversity("Otra Universidad");

        ProfessionalProfileResponse response = service.upsert(request);

        assertThat(response.getLicenseStatus()).isEqualTo(LicenseStatus.VERIFIED);
        verify(verificationRequestRepositoryPort, never()).save(any());
    }

    @Test
    void upsert_shouldAllowRetry_whenRejectedAndUnderAttemptLimit() {
        ProfessionalProfile existing = existingProfile(LicenseStatus.REJECTED);
        when(professionalProfileRepositoryPort.findByUserId(userId)).thenReturn(Optional.of(existing));
        when(professionalProfileRepositoryPort.existsByLicenseNumberAndUserIdNot(any(), eq(userId))).thenReturn(false);
        when(verificationRequestRepositoryPort.findLatestBySubject(SubjectType.PROFESSIONAL, existing.getId()))
                .thenReturn(Optional.of(VerificationRequest.builder().id(UUID.randomUUID())
                        .status(VerificationRequestStatus.REJECTED).attemptNumber(1).build()));

        ProfessionalProfileResponse response = service.upsert(validRequest());

        assertThat(response.getLicenseStatus()).isEqualTo(LicenseStatus.NOT_SUBMITTED);
        ArgumentCaptor<VerificationRequest> captor = ArgumentCaptor.forClass(VerificationRequest.class);
        verify(verificationRequestRepositoryPort).save(captor.capture());
        assertThat(captor.getValue().getAttemptNumber()).isEqualTo(2);
    }

    @Test
    void upsert_shouldThrowAttemptsExhausted_whenRejectedThirdTime() {
        ProfessionalProfile existing = existingProfile(LicenseStatus.REJECTED);
        when(professionalProfileRepositoryPort.findByUserId(userId)).thenReturn(Optional.of(existing));
        when(professionalProfileRepositoryPort.existsByLicenseNumberAndUserIdNot(any(), eq(userId))).thenReturn(false);
        when(verificationRequestRepositoryPort.findLatestBySubject(SubjectType.PROFESSIONAL, existing.getId()))
                .thenReturn(Optional.of(VerificationRequest.builder().id(UUID.randomUUID())
                        .status(VerificationRequestStatus.REJECTED).attemptNumber(3).build()));

        assertThatThrownBy(() -> service.upsert(validRequest()))
                .isInstanceOf(VerificationAttemptsExhaustedException.class);

        verify(professionalProfileRepositoryPort, never()).save(any());
    }

    @Test
    void upsert_shouldThrowLicenseNumberAlreadyExists_onUpdate_whenTakenByAnotherUser() {
        ProfessionalProfile existing = existingProfile(LicenseStatus.NOT_SUBMITTED);
        when(professionalProfileRepositoryPort.findByUserId(userId)).thenReturn(Optional.of(existing));
        when(professionalProfileRepositoryPort.existsByLicenseNumberAndUserIdNot("LIC-001", userId)).thenReturn(true);

        assertThatThrownBy(() -> service.upsert(validRequest()))
                .isInstanceOf(LicenseNumberAlreadyExistsException.class);
    }
}

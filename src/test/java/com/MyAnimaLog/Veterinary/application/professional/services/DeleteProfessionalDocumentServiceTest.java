package com.MyAnimaLog.Veterinary.application.professional.services;

import com.MyAnimaLog.Veterinary.application.professional.ports.out.ProfessionalProfileRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.dto.AuthenticatedUser;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.AuthenticatedUserPort;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.FileStoragePort;
import com.MyAnimaLog.Veterinary.application.verification.ports.out.VerificationDocumentRepositoryPort;
import com.MyAnimaLog.Veterinary.application.verification.ports.out.VerificationRequestRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.professional.model.ProfessionalProfile;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.DocumentNotInCorrectionException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VerificationLockedException;
import com.MyAnimaLog.Veterinary.domain.verification.enums.SubjectType;
import com.MyAnimaLog.Veterinary.domain.verification.enums.VerificationRequestStatus;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.VerificationDocumentNotFoundException;
import com.MyAnimaLog.Veterinary.domain.verification.model.VerificationDocument;
import com.MyAnimaLog.Veterinary.domain.verification.model.VerificationRequest;
import com.MyAnimaLog.Veterinary.infrastructure.storage.StorageProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteProfessionalDocumentServiceTest {

    @Mock
    private ProfessionalProfileRepositoryPort professionalProfileRepositoryPort;

    @Mock
    private VerificationRequestRepositoryPort verificationRequestRepositoryPort;

    @Mock
    private VerificationDocumentRepositoryPort verificationDocumentRepositoryPort;

    @Mock
    private AuthenticatedUserPort authenticatedUserPort;

    @Mock
    private FileStoragePort fileStoragePort;

    private DeleteProfessionalDocumentService service;
    private UUID userId;
    private ProfessionalProfile profile;
    private VerificationRequest openRequest;
    private VerificationDocument document;

    @BeforeEach
    void setUp() {
        service = new DeleteProfessionalDocumentService(professionalProfileRepositoryPort,
                verificationRequestRepositoryPort, verificationDocumentRepositoryPort,
                authenticatedUserPort, fileStoragePort, new StorageProperties());
        userId = UUID.randomUUID();
        profile = ProfessionalProfile.builder().id(UUID.randomUUID()).userId(userId).build();
        openRequest = VerificationRequest.builder().id(UUID.randomUUID()).status(VerificationRequestStatus.DRAFT).build();
        document = VerificationDocument.builder().id(UUID.randomUUID()).requestId(openRequest.getId())
                .documentType("PROFESSIONAL_CARD")
                .storagePath("verifications/" + openRequest.getId() + "/doc.pdf").build();

        lenient().when(authenticatedUserPort.current()).thenReturn(
                new AuthenticatedUser(userId, "vet@example.com", true, false, Optional.empty()));
        lenient().when(professionalProfileRepositoryPort.findByUserId(userId)).thenReturn(Optional.of(profile));
        lenient().when(verificationRequestRepositoryPort.findLatestBySubject(SubjectType.PROFESSIONAL, profile.getId()))
                .thenReturn(Optional.of(openRequest));
    }

    @Test
    void deleteDocument_shouldDeleteRowAndFile() {
        when(verificationDocumentRepositoryPort.findById(document.getId())).thenReturn(Optional.of(document));

        service.deleteDocument(document.getId());

        verify(verificationDocumentRepositoryPort, times(1)).deleteById(document.getId());
        verify(fileStoragePort, times(1)).delete("verification-documents", document.getStoragePath());
    }

    @Test
    void deleteDocument_shouldThrowNotFound_whenDocumentBelongsToAnotherRequest() {
        VerificationDocument otherRequestDoc = document.toBuilder().requestId(UUID.randomUUID()).build();
        when(verificationDocumentRepositoryPort.findById(document.getId())).thenReturn(Optional.of(otherRequestDoc));

        assertThatThrownBy(() -> service.deleteDocument(document.getId()))
                .isInstanceOf(VerificationDocumentNotFoundException.class);
    }

    @Test
    void deleteDocument_shouldThrowDocumentNotInCorrection_whenTypeWasNotRequested() {
        VerificationRequest needsCorrection = openRequest.toBuilder()
                .status(VerificationRequestStatus.NEEDS_CORRECTION)
                .correctionDocumentTypes(List.of("DEGREE"))
                .correctionRequestedAt(LocalDateTime.now())
                .build();
        when(verificationRequestRepositoryPort.findLatestBySubject(SubjectType.PROFESSIONAL, profile.getId()))
                .thenReturn(Optional.of(needsCorrection));
        when(verificationDocumentRepositoryPort.findById(document.getId())).thenReturn(Optional.of(document));

        assertThatThrownBy(() -> service.deleteDocument(document.getId()))
                .isInstanceOf(DocumentNotInCorrectionException.class);

        verify(verificationDocumentRepositoryPort, never()).deleteById(any());
    }

    @Test
    void deleteDocument_shouldThrowVerificationLocked_whenRequestIsUnderReview() {
        when(verificationRequestRepositoryPort.findLatestBySubject(SubjectType.PROFESSIONAL, profile.getId()))
                .thenReturn(Optional.of(VerificationRequest.builder().id(UUID.randomUUID())
                        .status(VerificationRequestStatus.UNDER_REVIEW).build()));

        assertThatThrownBy(() -> service.deleteDocument(document.getId()))
                .isInstanceOf(VerificationLockedException.class);
    }
}

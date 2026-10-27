package com.MyAnimaLog.Veterinary.application.verification.services;

import com.MyAnimaLog.Veterinary.application.shared.ports.out.FileStoragePort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.application.verification.dto.UploadVerificationDocumentResponse;
import com.MyAnimaLog.Veterinary.application.verification.ports.in.UploadVerificationDocumentUseCase;
import com.MyAnimaLog.Veterinary.application.verification.ports.out.VerificationDocumentRepositoryPort;
import com.MyAnimaLog.Veterinary.application.verification.ports.out.VerificationRequestRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.FileTooLargeException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.InvalidFileTypeException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VerificationLockedException;
import com.MyAnimaLog.Veterinary.domain.verification.enums.ClinicDocumentType;
import com.MyAnimaLog.Veterinary.domain.verification.enums.SubjectType;
import com.MyAnimaLog.Veterinary.domain.verification.enums.VerificationRequestStatus;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.InvalidDocumentTypeException;
import com.MyAnimaLog.Veterinary.domain.verification.model.VerificationDocument;
import com.MyAnimaLog.Veterinary.domain.verification.model.VerificationRequest;
import com.MyAnimaLog.Veterinary.infrastructure.storage.FileNameSanitizer;
import com.MyAnimaLog.Veterinary.infrastructure.storage.FileSignatureDetector;
import com.MyAnimaLog.Veterinary.infrastructure.storage.FileSignatureDetector.DetectedType;
import com.MyAnimaLog.Veterinary.infrastructure.storage.Sha256;
import com.MyAnimaLog.Veterinary.infrastructure.storage.StorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UploadVerificationDocumentService implements UploadVerificationDocumentUseCase {

    private static final long MAX_DOCUMENT_SIZE_BYTES = 10L * 1024 * 1024;
    private static final Set<DetectedType> ALLOWED_TYPES = Set.of(DetectedType.PDF, DetectedType.JPEG, DetectedType.PNG);

    private final VerificationRequestRepositoryPort verificationRequestRepositoryPort;
    private final VerificationDocumentRepositoryPort verificationDocumentRepositoryPort;
    private final VeterinaryAuthorizationService authorizationService;
    private final FileStoragePort fileStoragePort;
    private final StorageProperties storageProperties;

    @Override
    public UploadVerificationDocumentResponse uploadDocument(UUID veterinaryId, String documentType, byte[] content, String originalFileName) {
        authorizationService.requireVeterinaryOwner(veterinaryId);

        ClinicDocumentType type = parseDocumentType(documentType);

        VerificationRequest request = verificationRequestRepositoryPort.findLatestBySubject(SubjectType.CLINIC, veterinaryId)
                .filter(r -> r.getStatus() == VerificationRequestStatus.DRAFT || r.getStatus() == VerificationRequestStatus.NEEDS_CORRECTION)
                .orElseThrow(VerificationLockedException::new);
        CorrectionGate.checkDocumentTypeAllowed(request, type.name());

        if (content == null || content.length > MAX_DOCUMENT_SIZE_BYTES) {
            throw new FileTooLargeException("Document must be 10 MB or smaller");
        }
        DetectedType detectedType = FileSignatureDetector.detect(content)
                .filter(ALLOWED_TYPES::contains)
                .orElseThrow(() -> new InvalidFileTypeException("Document must be a PDF, JPEG, or PNG file"));

        Optional<VerificationDocument> existing = verificationDocumentRepositoryPort.findByRequestId(request.getId())
                .stream()
                .filter(d -> d.getDocumentType().equals(type.name()))
                .findFirst();

        UUID documentId = UUID.randomUUID();
        String storagePath = "verifications/" + request.getId() + "/" + documentId + "." + detectedType.extension();
        String bucket = storageProperties.getVerificationBucket();

        fileStoragePort.uploadPrivate(bucket, storagePath, content, detectedType.mimeType());

        VerificationDocument document = VerificationDocument.builder()
                .id(documentId)
                .requestId(request.getId())
                .documentType(type.name())
                .storagePath(storagePath)
                .mimeType(detectedType.mimeType())
                .fileSizeBytes((long) content.length)
                .sha256(Sha256.hex(content))
                .uploadedAt(LocalDateTime.now())
                .originalFileName(FileNameSanitizer.sanitize(originalFileName))
                .build();

        try {
            verificationDocumentRepositoryPort.save(document);
        } catch (RuntimeException e) {
            fileStoragePort.delete(bucket, storagePath);
            throw e;
        }

        existing.ifPresent(old -> {
            verificationDocumentRepositoryPort.deleteById(old.getId());
            fileStoragePort.delete(bucket, old.getStoragePath());
        });

        return UploadVerificationDocumentResponse.builder()
                .documentId(document.getId())
                .requestId(document.getRequestId())
                .documentType(document.getDocumentType())
                .fileName(document.displayFileName())
                .uploadedAt(document.getUploadedAt())
                .build();
    }

    private ClinicDocumentType parseDocumentType(String documentType) {
        try {
            return ClinicDocumentType.valueOf(documentType);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new InvalidDocumentTypeException(
                    "documentType must be one of RUT, CHAMBER_OF_COMMERCE, LEGAL_REP_ID, FACADE_PHOTO");
        }
    }
}

package com.MyAnimaLog.Veterinary.application.professional.services;

import com.MyAnimaLog.Veterinary.application.professional.ports.in.DeleteProfessionalDocumentUseCase;
import com.MyAnimaLog.Veterinary.application.professional.ports.out.ProfessionalProfileRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.dto.AuthenticatedUser;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.AuthenticatedUserPort;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.FileStoragePort;
import com.MyAnimaLog.Veterinary.application.verification.ports.out.VerificationDocumentRepositoryPort;
import com.MyAnimaLog.Veterinary.application.verification.ports.out.VerificationRequestRepositoryPort;
import com.MyAnimaLog.Veterinary.application.verification.services.CorrectionGate;
import com.MyAnimaLog.Veterinary.domain.professional.exceptions.ProfessionalProfileNotFoundException;
import com.MyAnimaLog.Veterinary.domain.professional.model.ProfessionalProfile;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VerificationLockedException;
import com.MyAnimaLog.Veterinary.domain.verification.enums.SubjectType;
import com.MyAnimaLog.Veterinary.domain.verification.enums.VerificationRequestStatus;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.VerificationDocumentNotFoundException;
import com.MyAnimaLog.Veterinary.domain.verification.model.VerificationDocument;
import com.MyAnimaLog.Veterinary.domain.verification.model.VerificationRequest;
import com.MyAnimaLog.Veterinary.infrastructure.storage.StorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeleteProfessionalDocumentService implements DeleteProfessionalDocumentUseCase {

    private final ProfessionalProfileRepositoryPort professionalProfileRepositoryPort;
    private final VerificationRequestRepositoryPort verificationRequestRepositoryPort;
    private final VerificationDocumentRepositoryPort verificationDocumentRepositoryPort;
    private final AuthenticatedUserPort authenticatedUserPort;
    private final FileStoragePort fileStoragePort;
    private final StorageProperties storageProperties;

    @Override
    public void deleteDocument(UUID documentId) {
        AuthenticatedUser caller = authenticatedUserPort.current();
        ProfessionalProfile profile = professionalProfileRepositoryPort.findByUserId(caller.userId())
                .orElseThrow(ProfessionalProfileNotFoundException::new);

        VerificationRequest request = verificationRequestRepositoryPort.findLatestBySubject(SubjectType.PROFESSIONAL, profile.getId())
                .filter(r -> r.getStatus() == VerificationRequestStatus.DRAFT || r.getStatus() == VerificationRequestStatus.NEEDS_CORRECTION)
                .orElseThrow(VerificationLockedException::new);

        VerificationDocument document = verificationDocumentRepositoryPort.findById(documentId)
                .filter(d -> d.getRequestId().equals(request.getId()))
                .orElseThrow(VerificationDocumentNotFoundException::new);
        CorrectionGate.checkDocumentTypeAllowed(request, document.getDocumentType());

        verificationDocumentRepositoryPort.deleteById(document.getId());
        fileStoragePort.delete(storageProperties.getVerificationBucket(), document.getStoragePath());
    }
}

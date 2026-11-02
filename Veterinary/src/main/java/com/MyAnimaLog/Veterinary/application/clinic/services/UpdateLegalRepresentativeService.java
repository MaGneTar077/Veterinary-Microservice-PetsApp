package com.MyAnimaLog.Veterinary.application.clinic.services;

import com.MyAnimaLog.Veterinary.application.clinic.dto.UpdateLegalRepresentativeRequest;
import com.MyAnimaLog.Veterinary.application.clinic.dto.UpdateLegalRepresentativeResponse;
import com.MyAnimaLog.Veterinary.application.clinic.ports.in.UpdateLegalRepresentativeUseCase;
import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.application.verification.ports.out.VerificationRequestRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VerificationLockedException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VeterinaryNotFoundException;
import com.MyAnimaLog.Veterinary.domain.verification.enums.SubjectType;
import com.MyAnimaLog.Veterinary.domain.verification.enums.VerificationRequestStatus;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.InvalidLegalRepresentativeException;
import com.MyAnimaLog.Veterinary.domain.verification.model.VerificationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdateLegalRepresentativeService implements UpdateLegalRepresentativeUseCase {

    private static final Set<String> VALID_DOCUMENT_TYPES = Set.of("CC", "CE", "PPT");

    private final VeterinaryRepositoryPort veterinaryRepositoryPort;
    private final VerificationRequestRepositoryPort verificationRequestRepositoryPort;
    private final VeterinaryAuthorizationService authorizationService;

    @Override
    public UpdateLegalRepresentativeResponse updateLegalRepresentative(UUID veterinaryId, UpdateLegalRepresentativeRequest request) {
        authorizationService.requireVeterinaryOwner(veterinaryId);

        Veterinary veterinary = veterinaryRepositoryPort.findById(veterinaryId)
                .orElseThrow(VeterinaryNotFoundException::new);

        VerificationRequestStatus latestStatus = verificationRequestRepositoryPort
                .findLatestBySubject(SubjectType.CLINIC, veterinaryId)
                .map(VerificationRequest::getStatus)
                .orElse(null);
        if (latestStatus != VerificationRequestStatus.DRAFT && latestStatus != VerificationRequestStatus.NEEDS_CORRECTION) {
            throw new VerificationLockedException();
        }

        validate(request);

        Veterinary updated = veterinary.toBuilder()
                .legalRepIsOwner(request.getIsOwner())
                .legalRepName(request.getFullName())
                .legalRepDocumentType(request.getDocumentType())
                .legalRepDocumentNumber(request.getDocumentNumber())
                .build();

        Veterinary saved = veterinaryRepositoryPort.save(updated);

        return UpdateLegalRepresentativeResponse.builder()
                .veterinaryId(saved.getId())
                .isOwner(saved.getLegalRepIsOwner())
                .fullName(saved.getLegalRepName())
                .documentType(saved.getLegalRepDocumentType())
                .documentNumber(saved.getLegalRepDocumentNumber())
                .build();
    }

    private void validate(UpdateLegalRepresentativeRequest request) {
        if (request.getIsOwner() == null) {
            throw new InvalidLegalRepresentativeException("isOwner is required");
        }
        if (request.getFullName() == null || request.getFullName().isBlank()) {
            throw new InvalidLegalRepresentativeException("fullName is required");
        }
        if (request.getDocumentType() == null || !VALID_DOCUMENT_TYPES.contains(request.getDocumentType())) {
            throw new InvalidLegalRepresentativeException("documentType must be one of CC, CE, PPT");
        }
        String documentNumber = request.getDocumentNumber();
        if (documentNumber == null || documentNumber.isBlank()) {
            throw new InvalidLegalRepresentativeException("documentNumber is required");
        }
        if ("CC".equals(request.getDocumentType()) && !documentNumber.matches("^\\d+$")) {
            throw new InvalidLegalRepresentativeException("documentNumber must contain only digits for CC");
        }
        if (!"CC".equals(request.getDocumentType()) && !documentNumber.matches("^[A-Za-z0-9]+$")) {
            throw new InvalidLegalRepresentativeException("documentNumber must be alphanumeric for CE and PPT");
        }
    }
}

package com.MyAnimaLog.Veterinary.application.professional.services;

import com.MyAnimaLog.Veterinary.application.professional.dto.ProfessionalProfileResponse;
import com.MyAnimaLog.Veterinary.application.professional.dto.UpsertProfessionalProfileRequest;
import com.MyAnimaLog.Veterinary.application.professional.ports.in.UpsertProfessionalProfileUseCase;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpsertProfessionalProfileService implements UpsertProfessionalProfileUseCase {

    private static final int MAX_ATTEMPTS = 3;
    private static final Set<String> VALID_DOCUMENT_TYPES = Set.of("CC", "CE", "PPT");
    private static final Set<String> VALID_PROFESSIONS = Set.of("MV", "MVZ");

    private final ProfessionalProfileRepositoryPort professionalProfileRepositoryPort;
    private final VerificationRequestRepositoryPort verificationRequestRepositoryPort;
    private final AuthenticatedUserPort authenticatedUserPort;

    @Override
    @Transactional
    public ProfessionalProfileResponse upsert(UpsertProfessionalProfileRequest request) {
        validate(request);

        AuthenticatedUser caller = authenticatedUserPort.current();
        UUID userId = caller.userId();

        Optional<ProfessionalProfile> existingOpt = professionalProfileRepositoryPort.findByUserId(userId);
        if (existingOpt.isEmpty()) {
            return create(userId, request);
        }
        return update(existingOpt.get(), userId, request);
    }

    private ProfessionalProfileResponse create(UUID userId, UpsertProfessionalProfileRequest request) {
        if (professionalProfileRepositoryPort.existsByLicenseNumber(request.getLicenseNumber())) {
            throw new LicenseNumberAlreadyExistsException();
        }

        ProfessionalProfile profile = ProfessionalProfile.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .fullName(request.getFullName())
                .documentType(IdDocumentType.valueOf(request.getDocumentType()))
                .documentNumber(request.getDocumentNumber())
                .profession(Profession.valueOf(request.getProfession()))
                .licenseNumber(request.getLicenseNumber())
                .specialty(request.getSpecialty())
                .university(request.getUniversity())
                .build();
        ProfessionalProfile saved = professionalProfileRepositoryPort.save(profile);

        verificationRequestRepositoryPort.save(VerificationRequest.builder()
                .id(UUID.randomUUID())
                .subjectType(SubjectType.PROFESSIONAL)
                .subjectId(saved.getId())
                .status(VerificationRequestStatus.DRAFT)
                .submittedBy(userId)
                .build());

        return GetMyProfessionalProfileService.toResponse(saved);
    }

    private ProfessionalProfileResponse update(ProfessionalProfile existing, UUID userId, UpsertProfessionalProfileRequest request) {
        if (existing.getLicenseStatus() == LicenseStatus.PENDING) {
            throw new VerificationLockedException();
        }
        if (professionalProfileRepositoryPort.existsByLicenseNumberAndUserIdNot(request.getLicenseNumber(), userId)) {
            throw new LicenseNumberAlreadyExistsException();
        }

        IdDocumentType newDocumentType = IdDocumentType.valueOf(request.getDocumentType());
        Profession newProfession = Profession.valueOf(request.getProfession());
        boolean identityChanged = !existing.getFullName().equals(request.getFullName())
                || existing.getDocumentType() != newDocumentType
                || !existing.getDocumentNumber().equals(request.getDocumentNumber())
                || existing.getProfession() != newProfession
                || !existing.getLicenseNumber().equals(request.getLicenseNumber());

        LicenseStatus newLicenseStatus = existing.getLicenseStatus();
        boolean needsNewRequest = false;
        Integer nextAttemptNumber = null;

        if (existing.getLicenseStatus() == LicenseStatus.VERIFIED && identityChanged) {
            newLicenseStatus = LicenseStatus.NOT_SUBMITTED;
            needsNewRequest = true;
            nextAttemptNumber = verificationRequestRepositoryPort.findLatestBySubject(SubjectType.PROFESSIONAL, existing.getId())
                    .map(r -> r.getAttemptNumber() + 1)
                    .orElse(1);
        } else if (existing.getLicenseStatus() == LicenseStatus.REJECTED) {
            Optional<VerificationRequest> latest = verificationRequestRepositoryPort.findLatestBySubject(SubjectType.PROFESSIONAL, existing.getId());
            int attemptNumber = latest.map(VerificationRequest::getAttemptNumber).orElse(0);
            if (attemptNumber >= MAX_ATTEMPTS) {
                throw new VerificationAttemptsExhaustedException();
            }
            newLicenseStatus = LicenseStatus.NOT_SUBMITTED;
            needsNewRequest = true;
            nextAttemptNumber = attemptNumber + 1;
        }

        ProfessionalProfile updated = existing.toBuilder()
                .fullName(request.getFullName())
                .documentType(newDocumentType)
                .documentNumber(request.getDocumentNumber())
                .profession(newProfession)
                .licenseNumber(request.getLicenseNumber())
                .specialty(request.getSpecialty())
                .university(request.getUniversity())
                .licenseStatus(newLicenseStatus)
                .updatedAt(LocalDateTime.now())
                .build();
        ProfessionalProfile saved = professionalProfileRepositoryPort.save(updated);

        if (needsNewRequest) {
            verificationRequestRepositoryPort.save(VerificationRequest.builder()
                    .id(UUID.randomUUID())
                    .subjectType(SubjectType.PROFESSIONAL)
                    .subjectId(saved.getId())
                    .status(VerificationRequestStatus.DRAFT)
                    .attemptNumber(nextAttemptNumber)
                    .submittedBy(userId)
                    .build());
        }

        return GetMyProfessionalProfileService.toResponse(saved);
    }

    private void validate(UpsertProfessionalProfileRequest request) {
        if (request.getFullName() == null || request.getFullName().isBlank()) {
            throw new InvalidProfessionalProfileException("fullName is required");
        }
        if (request.getDocumentType() == null || !VALID_DOCUMENT_TYPES.contains(request.getDocumentType())) {
            throw new InvalidProfessionalProfileException("documentType must be one of CC, CE, PPT");
        }
        if (request.getDocumentNumber() == null || request.getDocumentNumber().isBlank()) {
            throw new InvalidProfessionalProfileException("documentNumber is required");
        }
        if (request.getProfession() == null || !VALID_PROFESSIONS.contains(request.getProfession())) {
            throw new InvalidProfessionalProfileException("profession must be one of MV, MVZ");
        }
        if (request.getLicenseNumber() == null || request.getLicenseNumber().isBlank()) {
            throw new InvalidProfessionalProfileException("licenseNumber is required");
        }
    }
}

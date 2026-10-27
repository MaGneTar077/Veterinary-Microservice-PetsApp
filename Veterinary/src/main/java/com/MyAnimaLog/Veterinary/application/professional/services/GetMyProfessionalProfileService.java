package com.MyAnimaLog.Veterinary.application.professional.services;

import com.MyAnimaLog.Veterinary.application.professional.dto.ProfessionalProfileResponse;
import com.MyAnimaLog.Veterinary.application.professional.ports.in.GetMyProfessionalProfileUseCase;
import com.MyAnimaLog.Veterinary.application.professional.ports.out.ProfessionalProfileRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.dto.AuthenticatedUser;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.AuthenticatedUserPort;
import com.MyAnimaLog.Veterinary.application.verification.dto.VerificationDocumentStatusInfo;
import com.MyAnimaLog.Veterinary.application.verification.ports.out.VerificationDocumentRepositoryPort;
import com.MyAnimaLog.Veterinary.application.verification.ports.out.VerificationRequestRepositoryPort;
import com.MyAnimaLog.Veterinary.application.verification.services.VerificationDocumentStatusCalculator;
import com.MyAnimaLog.Veterinary.domain.professional.exceptions.ProfessionalProfileNotFoundException;
import com.MyAnimaLog.Veterinary.domain.professional.model.ProfessionalProfile;
import com.MyAnimaLog.Veterinary.domain.verification.enums.ProfessionalDocumentType;
import com.MyAnimaLog.Veterinary.domain.verification.enums.SubjectType;
import com.MyAnimaLog.Veterinary.domain.verification.model.VerificationDocument;
import com.MyAnimaLog.Veterinary.domain.verification.model.VerificationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetMyProfessionalProfileService implements GetMyProfessionalProfileUseCase {

    private final ProfessionalProfileRepositoryPort professionalProfileRepositoryPort;
    private final AuthenticatedUserPort authenticatedUserPort;
    private final VerificationRequestRepositoryPort verificationRequestRepositoryPort;
    private final VerificationDocumentRepositoryPort verificationDocumentRepositoryPort;

    @Override
    public ProfessionalProfileResponse getMyProfile() {
        AuthenticatedUser caller = authenticatedUserPort.current();
        ProfessionalProfile profile = professionalProfileRepositoryPort.findByUserId(caller.userId())
                .orElseThrow(ProfessionalProfileNotFoundException::new);

        ProfessionalProfileResponse response = toResponse(profile);

        Optional<VerificationRequest> latest = verificationRequestRepositoryPort.findLatestBySubject(SubjectType.PROFESSIONAL, profile.getId());
        if (latest.isEmpty()) {
            return response;
        }
        VerificationRequest request = latest.get();
        Map<String, VerificationDocument> documentsByType = verificationDocumentRepositoryPort.findByRequestId(request.getId())
                .stream()
                .collect(Collectors.toMap(VerificationDocument::getDocumentType, d -> d));

        List<VerificationDocumentStatusInfo> documents = new ArrayList<>();
        for (ProfessionalDocumentType type : ProfessionalDocumentType.values()) {
            VerificationDocument doc = documentsByType.get(type.name());
            documents.add(VerificationDocumentStatusInfo.builder()
                    .documentType(type.name())
                    .required(type.isRequired())
                    .status(VerificationDocumentStatusCalculator.statusFor(request, type.name(), doc))
                    .documentId(doc != null ? doc.getId() : null)
                    .fileName(doc != null ? doc.displayFileName() : null)
                    .uploadedAt(doc != null ? doc.getUploadedAt() : null)
                    .build());
        }
        response.setDocuments(documents);
        response.setRejectionReasonCode(request.getRejectionReasonCode());
        return response;
    }

    static ProfessionalProfileResponse toResponse(ProfessionalProfile profile) {
        return ProfessionalProfileResponse.builder()
                .id(profile.getId())
                .fullName(profile.getFullName())
                .documentType(profile.getDocumentType())
                .documentNumber(profile.getDocumentNumber())
                .profession(profile.getProfession())
                .licenseNumber(profile.getLicenseNumber())
                .specialty(profile.getSpecialty())
                .university(profile.getUniversity())
                .licenseStatus(profile.getLicenseStatus())
                .build();
    }
}

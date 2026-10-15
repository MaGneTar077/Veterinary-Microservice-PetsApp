package com.MyAnimaLog.Veterinary.infrastructure.verification.mapper;

import com.MyAnimaLog.Veterinary.domain.verification.model.VerificationRequest;
import com.MyAnimaLog.Veterinary.infrastructure.verification.entity.VerificationRequestEntity;
import org.springframework.stereotype.Component;

@Component
public class VerificationRequestMapper {

    public VerificationRequestEntity toEntity(VerificationRequest domain) {
        return VerificationRequestEntity.builder()
                .id(domain.getId())
                .subjectType(domain.getSubjectType())
                .subjectId(domain.getSubjectId())
                .status(domain.getStatus())
                .attemptNumber(domain.getAttemptNumber())
                .submittedBy(domain.getSubmittedBy())
                .submittedAt(domain.getSubmittedAt())
                .truthfulnessDeclaredAt(domain.getTruthfulnessDeclaredAt())
                .aiStatus(domain.getAiStatus())
                .aiReport(domain.getAiReport())
                .aiRiskLevel(domain.getAiRiskLevel())
                .reviewerId(domain.getReviewerId())
                .reviewerNotes(domain.getReviewerNotes())
                .decidedAt(domain.getDecidedAt())
                .registryChecked(domain.getRegistryChecked())
                .correctionDocumentTypes(domain.getCorrectionDocumentTypes())
                .correctionRequestedAt(domain.getCorrectionRequestedAt())
                .createdAt(domain.getCreatedAt())
                .build();
    }

    public VerificationRequest toDomain(VerificationRequestEntity entity) {
        return VerificationRequest.builder()
                .id(entity.getId())
                .subjectType(entity.getSubjectType())
                .subjectId(entity.getSubjectId())
                .status(entity.getStatus())
                .attemptNumber(entity.getAttemptNumber())
                .submittedBy(entity.getSubmittedBy())
                .submittedAt(entity.getSubmittedAt())
                .truthfulnessDeclaredAt(entity.getTruthfulnessDeclaredAt())
                .aiStatus(entity.getAiStatus())
                .aiReport(entity.getAiReport())
                .aiRiskLevel(entity.getAiRiskLevel())
                .reviewerId(entity.getReviewerId())
                .reviewerNotes(entity.getReviewerNotes())
                .decidedAt(entity.getDecidedAt())
                .registryChecked(entity.getRegistryChecked())
                .correctionDocumentTypes(entity.getCorrectionDocumentTypes())
                .correctionRequestedAt(entity.getCorrectionRequestedAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}

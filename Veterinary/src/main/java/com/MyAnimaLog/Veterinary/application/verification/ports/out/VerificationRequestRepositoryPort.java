package com.MyAnimaLog.Veterinary.application.verification.ports.out;

import com.MyAnimaLog.Veterinary.domain.verification.enums.SubjectType;
import com.MyAnimaLog.Veterinary.domain.verification.model.VerificationRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VerificationRequestRepositoryPort {
    Optional<VerificationRequest> findById(UUID id);
    List<VerificationRequest> findBySubjectTypeAndSubjectId(SubjectType subjectType, UUID subjectId);
    VerificationRequest save(VerificationRequest request);

    /** The most recently created request for a subject (DRAFT/UNDER_REVIEW/etc. — not filtered by status). */
    default Optional<VerificationRequest> findLatestBySubject(SubjectType subjectType, UUID subjectId) {
        List<VerificationRequest> requests = findBySubjectTypeAndSubjectId(subjectType, subjectId);
        return requests.isEmpty() ? Optional.empty() : Optional.of(requests.get(0));
    }
}

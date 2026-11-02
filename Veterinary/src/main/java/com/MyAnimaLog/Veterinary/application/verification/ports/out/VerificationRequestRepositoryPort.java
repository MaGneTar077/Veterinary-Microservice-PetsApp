package com.MyAnimaLog.Veterinary.application.verification.ports.out;

import com.MyAnimaLog.Veterinary.domain.verification.enums.SubjectType;
import com.MyAnimaLog.Veterinary.domain.verification.enums.VerificationRequestStatus;
import com.MyAnimaLog.Veterinary.domain.verification.model.VerificationRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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

    /** Page of requests for the admin queue (§1), ordered by {@code submittedAt} ascending. */
    AdminQueuePage findForAdminQueue(VerificationRequestStatus status, SubjectType subjectType, int page, int size);

    /** Counts per status (only UNDER_REVIEW/NEEDS_CORRECTION/APPROVED/REJECTED), respecting the subjectType filter. */
    Map<VerificationRequestStatus, Long> countByStatusForAdminQueue(SubjectType subjectType);

    /**
     * Atomically claims a request for a decision: flips its status from {@code expectedStatus} to
     * {@code newStatus} only if it is still {@code expectedStatus} in the database right now.
     * Returns {@code false} if another admin already decided it in the meantime (no rows updated) —
     * the caller should treat that as a conflict, not silently continue.
     */
    boolean claimForDecision(UUID requestId, VerificationRequestStatus expectedStatus, VerificationRequestStatus newStatus);

    /** REJECTED requests decided before {@code cutoff} — used by the 90-day document retention job. */
    List<VerificationRequest> findRejectedDecidedBefore(LocalDateTime cutoff);

    record AdminQueuePage(List<VerificationRequest> items, long total) {
    }
}

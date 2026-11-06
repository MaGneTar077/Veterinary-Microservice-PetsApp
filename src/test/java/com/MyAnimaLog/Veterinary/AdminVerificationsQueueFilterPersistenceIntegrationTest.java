package com.MyAnimaLog.Veterinary;

import com.MyAnimaLog.Veterinary.application.verification.ports.out.VerificationRequestRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.verification.enums.SubjectType;
import com.MyAnimaLog.Veterinary.domain.verification.enums.VerificationRequestStatus;
import com.MyAnimaLog.Veterinary.domain.verification.model.VerificationRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Reproduces and verifies the fix for the same bug on {@code GET /api/veterinary/admin/verifications}:
 * {@code VerificationRequestJpaRepository.findForAdminQueue}/{@code countByStatusForAdminQueue} used
 * JPQL {@code (:subjectType IS NULL OR ...)} guards. {@code status} is always supplied by
 * {@code ListAdminVerificationsService} (defaulted to {@code UNDER_REVIEW} when absent from the
 * request), but {@code subjectType} never had a default and is exactly the parameter that used to
 * 500 whenever the frontend called this endpoint without it. See
 * {@code AdminClinicsListingFilterPersistenceIntegrationTest} for the full explanation.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminVerificationsQueueFilterPersistenceIntegrationTest {

    @Autowired
    private VerificationRequestRepositoryPort verificationRequestRepositoryPort;

    private VerificationRequest persist(SubjectType subjectType, VerificationRequestStatus status) {
        return verificationRequestRepositoryPort.save(VerificationRequest.builder()
                .id(UUID.randomUUID())
                .subjectType(subjectType)
                .subjectId(UUID.randomUUID())
                .status(status)
                .submittedBy(UUID.randomUUID())
                .submittedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Test
    void findForAdminQueue_shouldNotFail_whenSubjectTypeIsAbsent() {
        persist(SubjectType.CLINIC, VerificationRequestStatus.UNDER_REVIEW);

        assertThatCode(() -> verificationRequestRepositoryPort.findForAdminQueue(
                VerificationRequestStatus.UNDER_REVIEW, null, 0, 50))
                .doesNotThrowAnyException();
    }

    @Test
    void findForAdminQueue_shouldNotFail_whenStatusIsAlsoAbsent() {
        persist(SubjectType.CLINIC, VerificationRequestStatus.UNDER_REVIEW);

        // Defensive: real callers always supply an effective status (ListAdminVerificationsService
        // defaults it to UNDER_REVIEW), but the repository layer itself must stay null-safe too.
        assertThatCode(() -> verificationRequestRepositoryPort.findForAdminQueue(null, null, 0, 50))
                .doesNotThrowAnyException();
    }

    @Test
    void findForAdminQueue_shouldFilterBySubjectTypeAndStatus() {
        VerificationRequest clinic = persist(SubjectType.CLINIC, VerificationRequestStatus.UNDER_REVIEW);
        persist(SubjectType.PROFESSIONAL, VerificationRequestStatus.UNDER_REVIEW);
        persist(SubjectType.CLINIC, VerificationRequestStatus.REJECTED);

        VerificationRequestRepositoryPort.AdminQueuePage page = verificationRequestRepositoryPort.findForAdminQueue(
                VerificationRequestStatus.UNDER_REVIEW, SubjectType.CLINIC, 0, 50);

        assertThat(page.items()).extracting(VerificationRequest::getId).contains(clinic.getId());
        assertThat(page.items()).allSatisfy(item -> {
            assertThat(item.getStatus()).isEqualTo(VerificationRequestStatus.UNDER_REVIEW);
            assertThat(item.getSubjectType()).isEqualTo(SubjectType.CLINIC);
        });
    }

    @Test
    void countByStatusForAdminQueue_shouldNotFail_whenSubjectTypeIsAbsent() {
        persist(SubjectType.CLINIC, VerificationRequestStatus.UNDER_REVIEW);

        assertThatCode(() -> verificationRequestRepositoryPort.countByStatusForAdminQueue(null))
                .doesNotThrowAnyException();
    }

    @Test
    void countByStatusForAdminQueue_shouldFilterBySubjectTypeOnly() {
        persist(SubjectType.PROFESSIONAL, VerificationRequestStatus.UNDER_REVIEW);
        persist(SubjectType.PROFESSIONAL, VerificationRequestStatus.REJECTED);

        var counts = verificationRequestRepositoryPort.countByStatusForAdminQueue(SubjectType.PROFESSIONAL);

        assertThat(counts.getOrDefault(VerificationRequestStatus.UNDER_REVIEW, 0L)).isGreaterThanOrEqualTo(1L);
        assertThat(counts.getOrDefault(VerificationRequestStatus.REJECTED, 0L)).isGreaterThanOrEqualTo(1L);
    }
}

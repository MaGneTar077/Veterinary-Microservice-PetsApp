package com.MyAnimaLog.Veterinary.infrastructure.verification.repositories;

import com.MyAnimaLog.Veterinary.domain.verification.enums.SubjectType;
import com.MyAnimaLog.Veterinary.domain.verification.enums.VerificationRequestStatus;
import com.MyAnimaLog.Veterinary.infrastructure.verification.entity.VerificationRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface VerificationRequestJpaRepository extends JpaRepository<VerificationRequestEntity, UUID>,
        JpaSpecificationExecutor<VerificationRequestEntity> {
    List<VerificationRequestEntity> findBySubjectTypeAndSubjectIdOrderByCreatedAtDesc(SubjectType subjectType, UUID subjectId);

    /**
     * {@code subjectType} present: the only two patterns actually called by {@code countByStatusForAdminQueue}'s
     * two call sites in the adapter — never a (:subjectType IS NULL OR ...) guard, which used to 500 on
     * PostgreSQL when the parameter had no value (see VerificationRequestAdminSpecifications for the
     * equivalent fix on findForAdminQueue, where a Specification is used instead since it's a Page of
     * entities rather than a GROUP BY aggregate).
     */
    @Query("SELECT v.status AS status, COUNT(v) AS total FROM VerificationRequestEntity v "
            + "WHERE v.subjectType = :subjectType "
            + "AND v.status IN ('UNDER_REVIEW', 'NEEDS_CORRECTION', 'APPROVED', 'REJECTED') "
            + "GROUP BY v.status")
    List<StatusCount> countByStatusForAdminQueueAndSubjectType(@Param("subjectType") SubjectType subjectType);

    @Query("SELECT v.status AS status, COUNT(v) AS total FROM VerificationRequestEntity v "
            + "WHERE v.status IN ('UNDER_REVIEW', 'NEEDS_CORRECTION', 'APPROVED', 'REJECTED') "
            + "GROUP BY v.status")
    List<StatusCount> countByStatusForAdminQueue();

    interface StatusCount {
        VerificationRequestStatus getStatus();
        long getTotal();
    }

    @Modifying(clearAutomatically = true)
    @Query("UPDATE VerificationRequestEntity v SET v.status = :newStatus WHERE v.id = :id AND v.status = :expectedStatus")
    int updateStatusIfCurrentlyIs(@Param("id") UUID id,
                                   @Param("expectedStatus") VerificationRequestStatus expectedStatus,
                                   @Param("newStatus") VerificationRequestStatus newStatus);

    List<VerificationRequestEntity> findByStatusAndDecidedAtBefore(VerificationRequestStatus status, LocalDateTime cutoff);
}

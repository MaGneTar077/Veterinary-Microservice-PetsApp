package com.MyAnimaLog.Veterinary.infrastructure.verification.repositories;

import com.MyAnimaLog.Veterinary.domain.verification.enums.SubjectType;
import com.MyAnimaLog.Veterinary.domain.verification.enums.VerificationRequestStatus;
import com.MyAnimaLog.Veterinary.infrastructure.verification.entity.VerificationRequestEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface VerificationRequestJpaRepository extends JpaRepository<VerificationRequestEntity, UUID> {
    List<VerificationRequestEntity> findBySubjectTypeAndSubjectIdOrderByCreatedAtDesc(SubjectType subjectType, UUID subjectId);

    @Query("SELECT v FROM VerificationRequestEntity v "
            + "WHERE v.status = :status AND (:subjectType IS NULL OR v.subjectType = :subjectType) "
            + "ORDER BY v.submittedAt ASC")
    Page<VerificationRequestEntity> findForAdminQueue(@Param("status") VerificationRequestStatus status,
                                                       @Param("subjectType") SubjectType subjectType,
                                                       Pageable pageable);

    @Query("SELECT v.status AS status, COUNT(v) AS total FROM VerificationRequestEntity v "
            + "WHERE (:subjectType IS NULL OR v.subjectType = :subjectType) "
            + "AND v.status IN ('UNDER_REVIEW', 'NEEDS_CORRECTION', 'APPROVED', 'REJECTED') "
            + "GROUP BY v.status")
    List<StatusCount> countByStatusForAdminQueue(@Param("subjectType") SubjectType subjectType);

    interface StatusCount {
        VerificationRequestStatus getStatus();
        long getTotal();
    }

    @Modifying(clearAutomatically = true)
    @Query("UPDATE VerificationRequestEntity v SET v.status = :newStatus WHERE v.id = :id AND v.status = :expectedStatus")
    int updateStatusIfCurrentlyIs(@Param("id") UUID id,
                                   @Param("expectedStatus") VerificationRequestStatus expectedStatus,
                                   @Param("newStatus") VerificationRequestStatus newStatus);
}

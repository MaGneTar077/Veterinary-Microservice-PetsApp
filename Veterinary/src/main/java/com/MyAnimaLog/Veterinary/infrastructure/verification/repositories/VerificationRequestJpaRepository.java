package com.MyAnimaLog.Veterinary.infrastructure.verification.repositories;

import com.MyAnimaLog.Veterinary.domain.verification.enums.SubjectType;
import com.MyAnimaLog.Veterinary.infrastructure.verification.entity.VerificationRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VerificationRequestJpaRepository extends JpaRepository<VerificationRequestEntity, UUID> {
    List<VerificationRequestEntity> findBySubjectTypeAndSubjectIdOrderByCreatedAtDesc(SubjectType subjectType, UUID subjectId);
}

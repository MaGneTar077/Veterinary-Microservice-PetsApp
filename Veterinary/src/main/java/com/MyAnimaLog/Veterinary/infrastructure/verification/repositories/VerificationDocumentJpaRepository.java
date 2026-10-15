package com.MyAnimaLog.Veterinary.infrastructure.verification.repositories;

import com.MyAnimaLog.Veterinary.infrastructure.verification.entity.VerificationDocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VerificationDocumentJpaRepository extends JpaRepository<VerificationDocumentEntity, UUID> {
    List<VerificationDocumentEntity> findByRequestId(UUID requestId);
}

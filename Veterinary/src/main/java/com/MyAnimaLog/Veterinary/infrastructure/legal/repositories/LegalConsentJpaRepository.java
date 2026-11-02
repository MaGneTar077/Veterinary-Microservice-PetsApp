package com.MyAnimaLog.Veterinary.infrastructure.legal.repositories;

import com.MyAnimaLog.Veterinary.infrastructure.legal.entity.LegalConsentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LegalConsentJpaRepository extends JpaRepository<LegalConsentEntity, UUID> {
    List<LegalConsentEntity> findByUserId(UUID userId);
}

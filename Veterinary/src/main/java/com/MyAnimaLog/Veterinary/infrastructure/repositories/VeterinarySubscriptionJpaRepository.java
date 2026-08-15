package com.MyAnimaLog.Veterinary.infrastructure.repositories;

import com.MyAnimaLog.Veterinary.infrastructure.entity.VeterinarySubscriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface VeterinarySubscriptionJpaRepository extends JpaRepository<VeterinarySubscriptionEntity, UUID> {
    Optional<VeterinarySubscriptionEntity> findByVeterinaryIdAndActiveTrue(UUID veterinaryId);
    Optional<VeterinarySubscriptionEntity> findTopByVeterinaryIdOrderByEndDateDesc(UUID veterinaryId);
    boolean existsByVeterinaryIdAndActiveTrue(UUID veterinaryId);
}

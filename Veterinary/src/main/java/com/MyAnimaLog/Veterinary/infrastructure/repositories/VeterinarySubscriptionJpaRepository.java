package com.MyAnimaLog.Veterinary.infrastructure.repositories;

import com.MyAnimaLog.Veterinary.infrastructure.entity.VeterinarySubscriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VeterinarySubscriptionJpaRepository extends JpaRepository<VeterinarySubscriptionEntity, UUID> {
    Optional<VeterinarySubscriptionEntity> findByVeterinaryIdAndActiveTrue(UUID veterinaryId);
}

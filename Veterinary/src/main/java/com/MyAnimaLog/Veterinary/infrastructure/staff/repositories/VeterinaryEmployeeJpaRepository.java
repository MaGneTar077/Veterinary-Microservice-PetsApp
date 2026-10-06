package com.MyAnimaLog.Veterinary.infrastructure.staff.repositories;

import com.MyAnimaLog.Veterinary.infrastructure.staff.entity.VeterinaryEmployeeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VeterinaryEmployeeJpaRepository extends JpaRepository<VeterinaryEmployeeEntity, UUID> {
    boolean existsByVeterinaryIdAndUserId(UUID veterinaryId, UUID userId);
    Optional<VeterinaryEmployeeEntity> findByVeterinaryIdAndUserId(UUID veterinaryId, UUID userId);
}

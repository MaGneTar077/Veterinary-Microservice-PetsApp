package com.MyAnimaLog.Veterinary.infrastructure.repositories;

import com.MyAnimaLog.Veterinary.infrastructure.entity.VeterinaryEmployeeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface VeterinaryEmployeeJpaRepository extends JpaRepository<VeterinaryEmployeeEntity, UUID> {
    boolean existsByVeterinaryIdAndUserId(UUID veterinaryId, UUID userId);
}

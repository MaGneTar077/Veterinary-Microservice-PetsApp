package com.MyAnimaLog.Veterinary.infrastructure.staff.repositories;

import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.infrastructure.staff.entity.VeterinaryEmployeeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VeterinaryEmployeeJpaRepository extends JpaRepository<VeterinaryEmployeeEntity, UUID> {
    boolean existsByVeterinaryIdAndUserId(UUID veterinaryId, UUID userId);
    Optional<VeterinaryEmployeeEntity> findByVeterinaryIdAndUserId(UUID veterinaryId, UUID userId);
    List<VeterinaryEmployeeEntity> findByUserIdAndRole(UUID userId, EmployeeRole role);
    List<VeterinaryEmployeeEntity> findByUserIdAndActiveTrue(UUID userId);
}

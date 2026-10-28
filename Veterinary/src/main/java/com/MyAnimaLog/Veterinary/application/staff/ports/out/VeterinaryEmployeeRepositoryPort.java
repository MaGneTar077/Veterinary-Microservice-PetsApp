package com.MyAnimaLog.Veterinary.application.staff.ports.out;

import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.domain.staff.model.VeterinaryEmployee;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VeterinaryEmployeeRepositoryPort {
    VeterinaryEmployee save(VeterinaryEmployee employee);
    boolean existsByVeterinaryIdAndUserId(UUID veterinaryId, UUID userId);
    Optional<VeterinaryEmployee> findById(UUID id);
    Optional<VeterinaryEmployee> findByVeterinaryIdAndUserId(UUID veterinaryId, UUID userId);
    List<VeterinaryEmployee> findByUserIdAndRole(UUID userId, EmployeeRole role);
    List<VeterinaryEmployee> findByUserIdAndActiveTrue(UUID userId);

    /** All employees of a single clinic, for the admin detail screen. */
    List<VeterinaryEmployee> findByVeterinaryId(UUID veterinaryId);

    /** All employees across a batch of clinics, for the admin list screen (avoids N+1 per row). */
    List<VeterinaryEmployee> findByVeterinaryIdIn(List<UUID> veterinaryIds);

    /** The clinic's active employee with the given role (used to resolve the OWNER). */
    Optional<VeterinaryEmployee> findByVeterinaryIdAndRoleAndActiveTrue(UUID veterinaryId, EmployeeRole role);
}

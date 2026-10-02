package com.MyAnimaLog.Veterinary.application.staff.ports.out;

import com.MyAnimaLog.Veterinary.domain.staff.model.VeterinaryEmployee;

import java.util.Optional;
import java.util.UUID;

public interface VeterinaryEmployeeRepositoryPort {
    VeterinaryEmployee save(VeterinaryEmployee employee);
    boolean existsByVeterinaryIdAndUserId(UUID veterinaryId, UUID userId);
    Optional<VeterinaryEmployee> findById(UUID id);
}

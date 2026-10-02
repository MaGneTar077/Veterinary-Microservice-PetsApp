package com.MyAnimaLog.Veterinary.application.patients.ports.out;

import com.MyAnimaLog.Veterinary.domain.patients.model.UserVeterinaryLink;

import java.util.Optional;
import java.util.UUID;

public interface UserVeterinaryLinkRepositoryPort {
    com.MyAnimaLog.Veterinary.domain.patients.model.UserVeterinaryLink save(
            com.MyAnimaLog.Veterinary.domain.patients.model.UserVeterinaryLink link);
    boolean existsByUserIdAndVeterinaryId(java.util.UUID userId, java.util.UUID veterinaryId);
    Optional<UserVeterinaryLink> findByUserIdAndVeterinaryId(UUID userId, UUID veterinaryId);
    void deleteById(UUID id);
}

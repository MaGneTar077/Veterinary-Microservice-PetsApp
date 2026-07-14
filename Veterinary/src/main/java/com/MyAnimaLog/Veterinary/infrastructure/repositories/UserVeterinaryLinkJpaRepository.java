package com.MyAnimaLog.Veterinary.infrastructure.repositories;

import com.MyAnimaLog.Veterinary.infrastructure.entity.UserVeterinaryLinkEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserVeterinaryLinkJpaRepository extends JpaRepository<UserVeterinaryLinkEntity, UUID> {
    boolean existsByUserIdAndVeterinaryId(UUID userId, UUID veterinaryId);
}

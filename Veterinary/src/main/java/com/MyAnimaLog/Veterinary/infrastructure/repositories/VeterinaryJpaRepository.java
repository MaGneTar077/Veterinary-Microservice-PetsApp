package com.MyAnimaLog.Veterinary.infrastructure.repositories;

import com.MyAnimaLog.Veterinary.infrastructure.entity.VeterinaryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VeterinaryJpaRepository extends JpaRepository<VeterinaryEntity, UUID> {
    boolean existsByEmail(String email);
    boolean existsByName(String name);
    boolean existsByInviteCode(String inviteCode);
    boolean existsByEmailAndIdNot(String email, UUID id);
    Optional<VeterinaryEntity> findByInviteCode(String inviteCode);
}
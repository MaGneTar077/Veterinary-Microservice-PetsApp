package com.MyAnimaLog.Veterinary.infrastructure.clinic.repositories;

import com.MyAnimaLog.Veterinary.infrastructure.clinic.entity.VeterinaryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VeterinaryJpaRepository extends JpaRepository<VeterinaryEntity, UUID>,
        JpaSpecificationExecutor<VeterinaryEntity> {
    boolean existsByEmail(String email);
    boolean existsByName(String name);
    boolean existsByInviteCode(String inviteCode);
    boolean existsByEmailAndIdNot(String email, UUID id);
    Optional<VeterinaryEntity> findByInviteCode(String inviteCode);
    Optional<VeterinaryEntity> findByInviteLink(String inviteLink);
    boolean existsByNit(String nit);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE VeterinaryEntity v SET v.status = :newStatus WHERE v.id = :id AND v.status = :expectedStatus")
    int updateStatusIfCurrentlyIs(@Param("id") UUID id,
                                   @Param("expectedStatus") String expectedStatus,
                                   @Param("newStatus") String newStatus);

    List<VeterinaryEntity> findByIdIn(List<UUID> ids);
}
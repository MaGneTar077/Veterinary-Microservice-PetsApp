package com.MyAnimaLog.Veterinary.infrastructure.clinic.repositories;

import com.MyAnimaLog.Veterinary.infrastructure.clinic.entity.VeterinaryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface VeterinaryJpaRepository extends JpaRepository<VeterinaryEntity, UUID> {
    boolean existsByEmail(String email);
    boolean existsByName(String name);
    boolean existsByInviteCode(String inviteCode);
    boolean existsByEmailAndIdNot(String email, UUID id);
    Optional<VeterinaryEntity> findByInviteCode(String inviteCode);
    Optional<VeterinaryEntity> findByInviteLink(String inviteLink);
    boolean existsByNit(String nit);

    @Query("SELECT v FROM VeterinaryEntity v WHERE "
            + "(:status IS NULL OR v.status = :status) AND "
            + "(:city IS NULL OR LOWER(v.city) = LOWER(:city)) AND "
            + "(:department IS NULL OR LOWER(v.department) = LOWER(:department)) AND "
            + "(:search IS NULL OR LOWER(v.name) LIKE :search OR LOWER(v.legalName) LIKE :search OR LOWER(v.nit) LIKE :search) AND "
            + "(:plan IS NULL OR EXISTS (SELECT 1 FROM VeterinarySubscriptionEntity s "
            + "    WHERE s.veterinaryId = v.id AND s.active = true AND s.plan = :plan)) "
            + "ORDER BY v.createdAt DESC")
    Page<VeterinaryEntity> findForAdminList(@Param("status") String status,
                                             @Param("search") String search,
                                             @Param("plan") String plan,
                                             @Param("city") String city,
                                             @Param("department") String department,
                                             Pageable pageable);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE VeterinaryEntity v SET v.status = :newStatus WHERE v.id = :id AND v.status = :expectedStatus")
    int updateStatusIfCurrentlyIs(@Param("id") UUID id,
                                   @Param("expectedStatus") String expectedStatus,
                                   @Param("newStatus") String newStatus);
}
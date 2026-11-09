package com.MyAnimaLog.Veterinary.infrastructure.patients.repositories;

import com.MyAnimaLog.Veterinary.domain.patients.enums.LinkStatus;
import com.MyAnimaLog.Veterinary.infrastructure.patients.entity.UserVeterinaryLinkEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserVeterinaryLinkJpaRepository extends JpaRepository<UserVeterinaryLinkEntity, UUID> {

    /** Como máximo una por el índice único parcial {@code ux_link_active_user_vet}. */
    Optional<UserVeterinaryLinkEntity> findByUserIdAndVeterinaryIdAndStatus(
            UUID userId, UUID veterinaryId, LinkStatus status);

    /**
     * {@code findFirst...OrderBy} y no un {@code Optional} liso: puede haber varias filas
     * {@code UNLINKED} del mismo par (la tabla no tuvo índice único hasta {@code 008_patients.sql}),
     * y una consulta que devuelva {@code Optional} revienta con más de un resultado.
     */
    Optional<UserVeterinaryLinkEntity> findFirstByUserIdAndVeterinaryIdOrderByLinkedAtDesc(
            UUID userId, UUID veterinaryId);

    List<UserVeterinaryLinkEntity> findByUserIdAndStatusOrderByLinkedAtDesc(UUID userId, LinkStatus status);

    Page<UserVeterinaryLinkEntity> findByVeterinaryIdAndStatus(UUID veterinaryId, LinkStatus status, Pageable pageable);
}

package com.MyAnimaLog.Veterinary.infrastructure.staff.repositories;

import com.MyAnimaLog.Veterinary.domain.staff.enums.InvitationStatus;
import com.MyAnimaLog.Veterinary.infrastructure.staff.entity.EmployeeInvitationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeInvitationJpaRepository extends JpaRepository<EmployeeInvitationEntity, UUID> {

    Optional<EmployeeInvitationEntity> findByTokenHash(String tokenHash);

    List<EmployeeInvitationEntity> findByVeterinaryIdAndStatusOrderByCreatedAtDesc(UUID veterinaryId,
                                                                                    InvitationStatus status);

    // El email se guarda ya normalizado en minúsculas, así que la comparación exacta alcanza
    // (el índice ix_employee_invitations_email es sobre lower(email), equivalente para estas filas).
    Optional<EmployeeInvitationEntity> findByVeterinaryIdAndEmailAndStatus(UUID veterinaryId, String email,
                                                                           InvitationStatus status);

    List<EmployeeInvitationEntity> findByEmailAndStatusOrderByCreatedAtDesc(String email, InvitationStatus status);
}

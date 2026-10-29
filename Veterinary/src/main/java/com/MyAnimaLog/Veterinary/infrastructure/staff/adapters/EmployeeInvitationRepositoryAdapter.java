package com.MyAnimaLog.Veterinary.infrastructure.staff.adapters;

import com.MyAnimaLog.Veterinary.application.staff.ports.out.EmployeeInvitationRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.staff.enums.InvitationStatus;
import com.MyAnimaLog.Veterinary.domain.staff.model.EmployeeInvitation;
import com.MyAnimaLog.Veterinary.infrastructure.staff.mapper.EmployeeInvitationMapper;
import com.MyAnimaLog.Veterinary.infrastructure.staff.repositories.EmployeeInvitationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class EmployeeInvitationRepositoryAdapter implements EmployeeInvitationRepositoryPort {

    private final EmployeeInvitationJpaRepository jpaRepository;
    private final EmployeeInvitationMapper mapper;

    @Override
    public EmployeeInvitation save(EmployeeInvitation invitation) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(invitation)));
    }

    @Override
    public Optional<EmployeeInvitation> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<EmployeeInvitation> findByTokenHash(String tokenHash) {
        return jpaRepository.findByTokenHash(tokenHash).map(mapper::toDomain);
    }

    @Override
    public List<EmployeeInvitation> findByVeterinaryIdAndStatus(UUID veterinaryId, InvitationStatus status) {
        return jpaRepository.findByVeterinaryIdAndStatusOrderByCreatedAtDesc(veterinaryId, status).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<EmployeeInvitation> findByVeterinaryIdAndEmailAndStatus(UUID veterinaryId, String email,
                                                                            InvitationStatus status) {
        return jpaRepository.findByVeterinaryIdAndEmailAndStatus(veterinaryId, email, status).map(mapper::toDomain);
    }

    @Override
    public List<EmployeeInvitation> findByEmailAndStatus(String email, InvitationStatus status) {
        return jpaRepository.findByEmailAndStatusOrderByCreatedAtDesc(email, status).stream()
                .map(mapper::toDomain)
                .toList();
    }
}

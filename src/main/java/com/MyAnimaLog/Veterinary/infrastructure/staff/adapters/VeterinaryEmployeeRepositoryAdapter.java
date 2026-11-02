package com.MyAnimaLog.Veterinary.infrastructure.staff.adapters;

import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.domain.staff.model.VeterinaryEmployee;
import com.MyAnimaLog.Veterinary.infrastructure.staff.mapper.VeterinaryEmployeeMapper;
import com.MyAnimaLog.Veterinary.infrastructure.staff.repositories.VeterinaryEmployeeJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class VeterinaryEmployeeRepositoryAdapter implements VeterinaryEmployeeRepositoryPort {

    private final VeterinaryEmployeeJpaRepository jpaRepository;
    private final VeterinaryEmployeeMapper mapper;

    @Override
    public VeterinaryEmployee save(VeterinaryEmployee employee) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(employee)));
    }

    @Override
    public boolean existsByVeterinaryIdAndUserId(UUID veterinaryId, UUID userId) {
        return jpaRepository.existsByVeterinaryIdAndUserId(veterinaryId, userId);
    }

    @Override
    public Optional<VeterinaryEmployee> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<VeterinaryEmployee> findByVeterinaryIdAndUserId(UUID veterinaryId, UUID userId) {
        return jpaRepository.findByVeterinaryIdAndUserId(veterinaryId, userId).map(mapper::toDomain);
    }

    @Override
    public List<VeterinaryEmployee> findByUserIdAndRole(UUID userId, EmployeeRole role) {
        return jpaRepository.findByUserIdAndRole(userId, role).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<VeterinaryEmployee> findByUserIdAndActiveTrue(UUID userId) {
        return jpaRepository.findByUserIdAndActiveTrue(userId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<VeterinaryEmployee> findByVeterinaryId(UUID veterinaryId) {
        return jpaRepository.findByVeterinaryId(veterinaryId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<VeterinaryEmployee> findByVeterinaryIdIn(List<UUID> veterinaryIds) {
        return jpaRepository.findByVeterinaryIdIn(veterinaryIds).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<VeterinaryEmployee> findByVeterinaryIdAndRoleAndActiveTrue(UUID veterinaryId, EmployeeRole role) {
        return jpaRepository.findByVeterinaryIdAndRoleAndActiveTrue(veterinaryId, role).map(mapper::toDomain);
    }

    @Override
    public List<VeterinaryEmployee> findByUserIdInAndActiveTrue(List<UUID> userIds) {
        return jpaRepository.findByUserIdInAndActiveTrue(userIds).stream().map(mapper::toDomain).toList();
    }

    @Override
    public boolean claimRoleTransition(UUID employeeId, EmployeeRole expectedRole, EmployeeRole newRole) {
        return jpaRepository.updateRoleIfCurrentlyIs(employeeId, expectedRole, newRole) == 1;
    }

    @Override
    public Optional<ClinicMembership> findClinicMembership(UUID veterinaryId, UUID userId) {
        return jpaRepository.findClinicMembership(veterinaryId, userId)
                .map(projection -> new ClinicMembership(
                        projection.getEmployeeId(),
                        projection.getRole(),
                        Boolean.TRUE.equals(projection.getActive()),
                        VeterinaryStatus.valueOf(projection.getVeterinaryStatus())));
    }
}

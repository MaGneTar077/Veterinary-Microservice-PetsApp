package com.MyAnimaLog.Veterinary.infrastructure.verification.adapters;

import com.MyAnimaLog.Veterinary.application.verification.ports.out.VerificationRequestRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.verification.enums.SubjectType;
import com.MyAnimaLog.Veterinary.domain.verification.enums.VerificationRequestStatus;
import com.MyAnimaLog.Veterinary.domain.verification.model.VerificationRequest;
import com.MyAnimaLog.Veterinary.infrastructure.verification.entity.VerificationRequestEntity;
import com.MyAnimaLog.Veterinary.infrastructure.verification.mapper.VerificationRequestMapper;
import com.MyAnimaLog.Veterinary.infrastructure.verification.repositories.VerificationRequestAdminSpecifications;
import com.MyAnimaLog.Veterinary.infrastructure.verification.repositories.VerificationRequestJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class VerificationRequestRepositoryAdapter implements VerificationRequestRepositoryPort {

    private final VerificationRequestJpaRepository jpaRepository;
    private final VerificationRequestMapper mapper;

    @Override
    public Optional<VerificationRequest> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<VerificationRequest> findBySubjectTypeAndSubjectId(SubjectType subjectType, UUID subjectId) {
        return jpaRepository.findBySubjectTypeAndSubjectIdOrderByCreatedAtDesc(subjectType, subjectId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public VerificationRequest save(VerificationRequest request) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(request)));
    }

    @Override
    public AdminQueuePage findForAdminQueue(VerificationRequestStatus status, SubjectType subjectType, int page, int size) {
        Specification<VerificationRequestEntity> specification =
                VerificationRequestAdminSpecifications.build(status, subjectType);
        Page<VerificationRequestEntity> result =
                jpaRepository.findAll(specification, PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "submittedAt")));
        List<VerificationRequest> items = result.getContent().stream().map(mapper::toDomain).toList();
        return new AdminQueuePage(items, result.getTotalElements());
    }

    @Override
    public boolean claimForDecision(UUID requestId, VerificationRequestStatus expectedStatus, VerificationRequestStatus newStatus) {
        return jpaRepository.updateStatusIfCurrentlyIs(requestId, expectedStatus, newStatus) == 1;
    }

    @Override
    public Map<VerificationRequestStatus, Long> countByStatusForAdminQueue(SubjectType subjectType) {
        Map<VerificationRequestStatus, Long> counts = new EnumMap<>(VerificationRequestStatus.class);
        List<VerificationRequestJpaRepository.StatusCount> rows = subjectType != null
                ? jpaRepository.countByStatusForAdminQueueAndSubjectType(subjectType)
                : jpaRepository.countByStatusForAdminQueue();
        for (VerificationRequestJpaRepository.StatusCount row : rows) {
            counts.put(row.getStatus(), row.getTotal());
        }
        return counts;
    }

    @Override
    public List<VerificationRequest> findRejectedDecidedBefore(LocalDateTime cutoff) {
        return jpaRepository.findByStatusAndDecidedAtBefore(VerificationRequestStatus.REJECTED, cutoff).stream()
                .map(mapper::toDomain)
                .toList();
    }
}

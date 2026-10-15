package com.MyAnimaLog.Veterinary.infrastructure.verification.adapters;

import com.MyAnimaLog.Veterinary.application.verification.ports.out.VerificationRequestRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.verification.enums.SubjectType;
import com.MyAnimaLog.Veterinary.domain.verification.model.VerificationRequest;
import com.MyAnimaLog.Veterinary.infrastructure.verification.mapper.VerificationRequestMapper;
import com.MyAnimaLog.Veterinary.infrastructure.verification.repositories.VerificationRequestJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
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
}

package com.MyAnimaLog.Veterinary.infrastructure.verification.adapters;

import com.MyAnimaLog.Veterinary.application.verification.ports.out.VerificationDocumentRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.verification.model.VerificationDocument;
import com.MyAnimaLog.Veterinary.infrastructure.verification.mapper.VerificationDocumentMapper;
import com.MyAnimaLog.Veterinary.infrastructure.verification.repositories.VerificationDocumentJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class VerificationDocumentRepositoryAdapter implements VerificationDocumentRepositoryPort {

    private final VerificationDocumentJpaRepository jpaRepository;
    private final VerificationDocumentMapper mapper;

    @Override
    public Optional<VerificationDocument> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<VerificationDocument> findByRequestId(UUID requestId) {
        return jpaRepository.findByRequestId(requestId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<VerificationDocument> findBySha256(String sha256) {
        return jpaRepository.findBySha256(sha256).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public VerificationDocument save(VerificationDocument document) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(document)));
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}

package com.MyAnimaLog.Veterinary.infrastructure.patients.adapters;

import com.MyAnimaLog.Veterinary.application.patients.ports.out.UserVeterinaryLinkRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.patients.model.UserVeterinaryLink;
import com.MyAnimaLog.Veterinary.infrastructure.patients.mapper.UserVeterinaryLinkMapper;
import com.MyAnimaLog.Veterinary.infrastructure.patients.repositories.UserVeterinaryLinkJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserVeterinaryLinkRepositoryAdapter implements UserVeterinaryLinkRepositoryPort {

    private final UserVeterinaryLinkJpaRepository jpaRepository;
    private final UserVeterinaryLinkMapper mapper;

    @Override
    public UserVeterinaryLink save(UserVeterinaryLink link) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(link)));
    }

    @Override
    public boolean existsByUserIdAndVeterinaryId(UUID userId, UUID veterinaryId) {
        return jpaRepository.existsByUserIdAndVeterinaryId(userId, veterinaryId);
    }

    @Override
    public Optional<UserVeterinaryLink> findByUserIdAndVeterinaryId(UUID userId, UUID veterinaryId) {
        return jpaRepository.findByUserIdAndVeterinaryId(userId, veterinaryId)
                .map(mapper::toDomain);
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}
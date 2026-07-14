package com.MyAnimaLog.Veterinary.infrastructure.adapters;

import com.MyAnimaLog.Veterinary.application.ports.out.UserVeterinaryLinkRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.model.UserVeterinaryLink;
import com.MyAnimaLog.Veterinary.infrastructure.mapper.UserVeterinaryLinkMapper;
import com.MyAnimaLog.Veterinary.infrastructure.repositories.UserVeterinaryLinkJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
}
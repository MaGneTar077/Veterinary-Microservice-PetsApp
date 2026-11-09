package com.MyAnimaLog.Veterinary.infrastructure.patients.adapters;

import com.MyAnimaLog.Veterinary.application.patients.ports.out.UserVeterinaryLinkRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.patients.enums.LinkStatus;
import com.MyAnimaLog.Veterinary.domain.patients.model.UserVeterinaryLink;
import com.MyAnimaLog.Veterinary.infrastructure.patients.mapper.UserVeterinaryLinkMapper;
import com.MyAnimaLog.Veterinary.infrastructure.patients.repositories.UserVeterinaryLinkJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
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
    public Optional<UserVeterinaryLink> findActiveLink(UUID userId, UUID veterinaryId) {
        return jpaRepository.findByUserIdAndVeterinaryIdAndStatus(userId, veterinaryId, LinkStatus.LINKED)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<UserVeterinaryLink> findLatestLink(UUID userId, UUID veterinaryId) {
        return jpaRepository.findFirstByUserIdAndVeterinaryIdOrderByLinkedAtDesc(userId, veterinaryId)
                .map(mapper::toDomain);
    }

    @Override
    public List<UserVeterinaryLink> findActiveByUserId(UUID userId) {
        return jpaRepository.findByUserIdAndStatusOrderByLinkedAtDesc(userId, LinkStatus.LINKED).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public ClientsPage findActiveByVeterinaryId(UUID veterinaryId, int page, int size) {
        Page<com.MyAnimaLog.Veterinary.infrastructure.patients.entity.UserVeterinaryLinkEntity> result =
                jpaRepository.findByVeterinaryIdAndStatus(veterinaryId, LinkStatus.LINKED,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "linkedAt")));
        return new ClientsPage(result.getContent().stream().map(mapper::toDomain).toList(),
                result.getTotalElements());
    }
}

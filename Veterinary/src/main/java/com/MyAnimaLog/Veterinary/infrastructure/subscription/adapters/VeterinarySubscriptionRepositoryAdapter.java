package com.MyAnimaLog.Veterinary.infrastructure.subscription.adapters;

import com.MyAnimaLog.Veterinary.application.subscription.ports.out.VeterinarySubscriptionRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.subscription.model.VeterinarySubscription;
import com.MyAnimaLog.Veterinary.infrastructure.subscription.mapper.VeterinarySubscriptionMapper;
import com.MyAnimaLog.Veterinary.infrastructure.subscription.repositories.VeterinarySubscriptionJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class VeterinarySubscriptionRepositoryAdapter implements VeterinarySubscriptionRepositoryPort {

    private final VeterinarySubscriptionJpaRepository jpaRepository;
    private final VeterinarySubscriptionMapper mapper;

    @Override
    public Optional<VeterinarySubscription> findActiveByVeterinaryId(UUID veterinaryId) {
        return jpaRepository.findByVeterinaryIdAndActiveTrue(veterinaryId)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<VeterinarySubscription> findLatestByVeterinaryId(UUID veterinaryId) {
        return jpaRepository.findTopByVeterinaryIdOrderByEndDateDesc(veterinaryId)
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsActiveByVeterinaryId(UUID veterinaryId) {
        return jpaRepository.existsByVeterinaryIdAndActiveTrue(veterinaryId);
    }

    @Override
    public VeterinarySubscription save(VeterinarySubscription subscription) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(subscription)));
    }
}
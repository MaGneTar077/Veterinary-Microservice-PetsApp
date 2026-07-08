package com.MyAnimaLog.Veterinary.infrastructure.adapters;

import com.MyAnimaLog.Veterinary.application.ports.out.VeterinarySubscriptionRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.model.VeterinarySubscription;
import com.MyAnimaLog.Veterinary.infrastructure.mapper.VeterinarySubscriptionMapper;
import com.MyAnimaLog.Veterinary.infrastructure.repositories.VeterinarySubscriptionJpaRepository;
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
}
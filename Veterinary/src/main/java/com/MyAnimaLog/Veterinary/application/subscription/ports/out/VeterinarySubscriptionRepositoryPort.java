package com.MyAnimaLog.Veterinary.application.subscription.ports.out;

import com.MyAnimaLog.Veterinary.domain.subscription.model.VeterinarySubscription;

import java.util.Optional;
import java.util.UUID;

public interface VeterinarySubscriptionRepositoryPort {
    Optional<VeterinarySubscription> findActiveByVeterinaryId(UUID veterinaryId);
    Optional<VeterinarySubscription> findLatestByVeterinaryId(UUID veterinaryId);
    boolean existsActiveByVeterinaryId(UUID veterinaryId);
    VeterinarySubscription save(VeterinarySubscription subscription);
}

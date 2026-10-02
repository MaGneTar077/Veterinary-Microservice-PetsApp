package com.MyAnimaLog.Veterinary.infrastructure.subscription.mapper;

import com.MyAnimaLog.Veterinary.domain.subscription.model.VeterinarySubscription;
import com.MyAnimaLog.Veterinary.infrastructure.subscription.entity.VeterinarySubscriptionEntity;
import org.springframework.stereotype.Component;

@Component
public class VeterinarySubscriptionMapper {

    public VeterinarySubscriptionEntity toEntity(VeterinarySubscription domain) {
        return VeterinarySubscriptionEntity.builder()
                .id(domain.getId())
                .veterinaryId(domain.getVeterinaryId())
                .plan(domain.getPlan())
                .startDate(domain.getStartDate())
                .endDate(domain.getEndDate())
                .active(domain.getActive())
                .build();
    }

    public VeterinarySubscription toDomain(VeterinarySubscriptionEntity entity) {
        return VeterinarySubscription.builder()
                .id(entity.getId())
                .veterinaryId(entity.getVeterinaryId())
                .plan(entity.getPlan())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .active(entity.getActive())
                .build();
    }
}
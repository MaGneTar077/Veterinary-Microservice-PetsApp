package com.MyAnimaLog.Veterinary.application.services;

import com.MyAnimaLog.Veterinary.application.dto.GetActivePlanResponse;
import com.MyAnimaLog.Veterinary.application.ports.in.GetActivePlanUseCase;
import com.MyAnimaLog.Veterinary.application.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.ports.out.VeterinarySubscriptionRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.exceptions.NoActiveSubscriptionException;
import com.MyAnimaLog.Veterinary.domain.exceptions.VeterinaryNotFoundException;
import com.MyAnimaLog.Veterinary.domain.model.VeterinarySubscription;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetActivePlanService implements GetActivePlanUseCase {

    private final VeterinaryRepositoryPort veterinaryRepositoryPort;
    private final VeterinarySubscriptionRepositoryPort subscriptionRepositoryPort;


    @Override
    public GetActivePlanResponse getActivePlan(UUID veterinaryId) {

        veterinaryRepositoryPort.findById(veterinaryId)
                .orElseThrow(VeterinaryNotFoundException::new);

        VeterinarySubscription subscription = subscriptionRepositoryPort
                .findActiveByVeterinaryId(veterinaryId)
                .orElseThrow(NoActiveSubscriptionException::new);


        if (subscription.getEndDate().isBefore(LocalDate.now())) {
            throw new NoActiveSubscriptionException("Subscription has expired");
        }

        return GetActivePlanResponse.builder()
                .id(subscription.getId())
                .veterinaryId(subscription.getVeterinaryId())
                .plan(subscription.getPlan())
                .startDate(subscription.getStartDate())
                .endDate(subscription.getEndDate())
                .active(subscription.getActive())
                .build();
    }
}

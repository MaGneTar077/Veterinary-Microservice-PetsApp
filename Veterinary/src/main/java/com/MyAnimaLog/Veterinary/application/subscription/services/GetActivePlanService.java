package com.MyAnimaLog.Veterinary.application.subscription.services;

import com.MyAnimaLog.Veterinary.application.subscription.dto.GetActivePlanResponse;
import com.MyAnimaLog.Veterinary.application.subscription.ports.in.GetActivePlanUseCase;
import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.subscription.ports.out.VeterinarySubscriptionRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.domain.subscription.exceptions.NoActiveSubscriptionException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VeterinaryNotFoundException;
import com.MyAnimaLog.Veterinary.domain.subscription.model.VeterinarySubscription;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetActivePlanService implements GetActivePlanUseCase {

    private final VeterinaryRepositoryPort veterinaryRepositoryPort;
    private final VeterinarySubscriptionRepositoryPort subscriptionRepositoryPort;
    private final VeterinaryAuthorizationService authorizationService;


    @Override
    public GetActivePlanResponse getActivePlan(UUID veterinaryId) {
        authorizationService.requireMember(veterinaryId);

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

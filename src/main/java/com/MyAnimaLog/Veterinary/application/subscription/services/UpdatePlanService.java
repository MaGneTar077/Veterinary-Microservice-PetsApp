package com.MyAnimaLog.Veterinary.application.subscription.services;

import com.MyAnimaLog.Veterinary.application.subscription.dto.UpdatePlanRequest;
import com.MyAnimaLog.Veterinary.application.subscription.dto.UpdatePlanResponse;
import com.MyAnimaLog.Veterinary.application.subscription.ports.in.UpdatePlanUseCase;
import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.subscription.ports.out.VeterinarySubscriptionRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.domain.subscription.exceptions.NoActiveSubscriptionException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VeterinaryNotFoundException;
import com.MyAnimaLog.Veterinary.domain.subscription.model.VeterinarySubscription;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdatePlanService implements UpdatePlanUseCase {

    private final VeterinaryRepositoryPort veterinaryRepositoryPort;
    private final VeterinarySubscriptionRepositoryPort subscriptionRepositoryPort;
    private final VeterinaryAuthorizationService authorizationService;

    @Override
    public UpdatePlanResponse updatePlan(UUID veterinaryId, UpdatePlanRequest request) {
        authorizationService.requirePlatformAdmin();

        veterinaryRepositoryPort.findById(veterinaryId)
                .orElseThrow(VeterinaryNotFoundException::new);

        if (request.getStartDate() != null && request.getEndDate() != null
                && request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }

        VeterinarySubscription subscription = subscriptionRepositoryPort
                .findActiveByVeterinaryId(veterinaryId)
                .orElseThrow(NoActiveSubscriptionException::new);

        VeterinarySubscription updated = subscription.toBuilder()
                .plan(request.getPlan() != null ? request.getPlan() : subscription.getPlan())
                .startDate(request.getStartDate() != null ? request.getStartDate() : subscription.getStartDate())
                .endDate(request.getEndDate() != null ? request.getEndDate() : subscription.getEndDate())
                .build();

        VeterinarySubscription saved = subscriptionRepositoryPort.save(updated);

        return UpdatePlanResponse.builder()
                .id(saved.getId())
                .veterinaryId(saved.getVeterinaryId())
                .plan(saved.getPlan())
                .startDate(saved.getStartDate())
                .endDate(saved.getEndDate())
                .active(saved.getActive())
                .build();
    }
}

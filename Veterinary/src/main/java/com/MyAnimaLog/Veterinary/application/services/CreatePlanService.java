package com.MyAnimaLog.Veterinary.application.services;

import com.MyAnimaLog.Veterinary.application.dto.CreatePlanRequest;
import com.MyAnimaLog.Veterinary.application.dto.CreatePlanResponse;
import com.MyAnimaLog.Veterinary.application.ports.in.CreatePlanUseCase;
import com.MyAnimaLog.Veterinary.application.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.ports.out.VeterinarySubscriptionRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.exceptions.ActiveSubscriptionAlreadyExistsException;
import com.MyAnimaLog.Veterinary.domain.exceptions.InvalidVeterinaryNameException;
import com.MyAnimaLog.Veterinary.domain.exceptions.VeterinaryNotActiveException;
import com.MyAnimaLog.Veterinary.domain.exceptions.VeterinaryNotFoundException;
import com.MyAnimaLog.Veterinary.domain.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.model.VeterinarySubscription;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreatePlanService implements CreatePlanUseCase {

    private final VeterinaryRepositoryPort veterinaryRepositoryPort;
    private final VeterinarySubscriptionRepositoryPort subscriptionRepositoryPort;


    @Override
    public CreatePlanResponse createPlan(CreatePlanRequest request) {

        if (request.getPlan() == null || request.getPlan().isBlank()) {
            throw new InvalidVeterinaryNameException("Plan name is required");
        }
        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new IllegalArgumentException("Start date and end date are required");
        }
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }

        Veterinary veterinary = veterinaryRepositoryPort.findById(request.getVeterinaryId())
                .orElseThrow(VeterinaryNotFoundException::new);

        if (!veterinary.getActive()) {
            throw new VeterinaryNotActiveException();
        }

        if (subscriptionRepositoryPort.existsActiveByVeterinaryId(request.getVeterinaryId())) {
            throw new ActiveSubscriptionAlreadyExistsException();
        }

        VeterinarySubscription subscription = VeterinarySubscription.builder()
                .id(UUID.randomUUID())
                .veterinaryId(request.getVeterinaryId())
                .plan(request.getPlan())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .active(true)
                .build();

        VeterinarySubscription saved = subscriptionRepositoryPort.save(subscription);

        return CreatePlanResponse.builder()
                .id(saved.getId())
                .veterinaryId(saved.getVeterinaryId())
                .plan(saved.getPlan())
                .startDate(saved.getStartDate())
                .endDate(saved.getEndDate())
                .active(saved.getActive())
                .build();
    }
}

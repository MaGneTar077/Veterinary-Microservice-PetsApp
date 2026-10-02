package com.MyAnimaLog.Veterinary.application.subscription.services;

import com.MyAnimaLog.Veterinary.application.subscription.dto.IsExpiredResponse;
import com.MyAnimaLog.Veterinary.application.subscription.ports.in.IsExpiredUseCase;
import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.subscription.ports.out.VeterinarySubscriptionRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.subscription.exceptions.SubscriptionNotFoundException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VeterinaryNotFoundException;
import com.MyAnimaLog.Veterinary.domain.subscription.model.VeterinarySubscription;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IsExpiredService  implements IsExpiredUseCase {

    private final VeterinaryRepositoryPort veterinaryRepositoryPort;
    private final VeterinarySubscriptionRepositoryPort subscriptionRepositoryPort;


    @Override
    public IsExpiredResponse isExpired(UUID veterinaryId) {

        veterinaryRepositoryPort.findById(veterinaryId)
                .orElseThrow(VeterinaryNotFoundException::new);

        VeterinarySubscription subscription = subscriptionRepositoryPort
                .findLatestByVeterinaryId(veterinaryId)
                .orElseThrow(SubscriptionNotFoundException::new);

        boolean expired = subscription.getEndDate().isBefore(LocalDate.now());

        return IsExpiredResponse.builder()
                .veterinaryId(veterinaryId)
                .plan(subscription.getPlan())
                .endDate(subscription.getEndDate())
                .expired(expired)
                .build();
    }
}

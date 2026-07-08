package com.MyAnimaLog.Veterinary.application.services;

import com.MyAnimaLog.Veterinary.application.dto.IsExpiredResponse;
import com.MyAnimaLog.Veterinary.application.ports.in.IsExpiredUseCase;
import com.MyAnimaLog.Veterinary.application.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.ports.out.VeterinarySubscriptionRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.exceptions.SubscriptionNotFoundException;
import com.MyAnimaLog.Veterinary.domain.exceptions.VeterinaryNotFoundException;
import com.MyAnimaLog.Veterinary.domain.model.VeterinarySubscription;
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

package com.MyAnimaLog.Veterinary.application.clinic.services;

import com.MyAnimaLog.Veterinary.application.clinic.dto.GetVeterinaryMemberResponse;
import com.MyAnimaLog.Veterinary.application.clinic.ports.in.GetVeterinaryMemberUseCase;
import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.application.subscription.ports.out.VeterinarySubscriptionRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.staff.model.VeterinaryEmployee;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Consumed by the User service (over {@code /internal/**}) to decide whether a user can obtain
 * a clinic (veterinary) token context, and with which role/status — CONTRATOS_COMPARTIDOS.md §2.
 */
@Service
@RequiredArgsConstructor
public class GetVeterinaryMemberService implements GetVeterinaryMemberUseCase {

    private static final String SUBSCRIPTION_ACTIVE = "ACTIVE";
    private static final String SUBSCRIPTION_NONE = "NONE";

    private final VeterinaryRepositoryPort veterinaryRepositoryPort;
    private final VeterinaryEmployeeRepositoryPort employeeRepositoryPort;
    private final VeterinarySubscriptionRepositoryPort subscriptionRepositoryPort;

    @Override
    public GetVeterinaryMemberResponse getMember(UUID veterinaryId, UUID userId) {
        Optional<Veterinary> veterinary = veterinaryRepositoryPort.findById(veterinaryId);
        if (veterinary.isEmpty()) {
            // La clínica no existe: se trata igual que "no es miembro", no es un 404.
            return GetVeterinaryMemberResponse.builder()
                    .member(false)
                    .licensed(false)
                    .build();
        }

        VeterinaryStatus status = VeterinaryStatus.fromActiveFlag(veterinary.get().getActive());
        String subscriptionStatus = subscriptionRepositoryPort.existsActiveByVeterinaryId(veterinaryId)
                ? SUBSCRIPTION_ACTIVE
                : SUBSCRIPTION_NONE;

        Optional<VeterinaryEmployee> employee = employeeRepositoryPort.findByVeterinaryIdAndUserId(veterinaryId, userId);
        if (employee.isEmpty()) {
            return GetVeterinaryMemberResponse.builder()
                    .member(false)
                    .licensed(false)
                    .veterinaryStatus(status)
                    .subscriptionStatus(subscriptionStatus)
                    .build();
        }

        VeterinaryEmployee veterinaryEmployee = employee.get();
        return GetVeterinaryMemberResponse.builder()
                .member(true)
                .employeeId(veterinaryEmployee.getId())
                .role(veterinaryEmployee.getRole())
                .active(veterinaryEmployee.getActive())
                // TODO(profesional): hoy no existe perfil profesional; vet_licensed siempre false
                // hasta que exista una verificación real de tarjeta profesional.
                .licensed(false)
                .veterinaryStatus(status)
                .subscriptionStatus(subscriptionStatus)
                .build();
    }
}

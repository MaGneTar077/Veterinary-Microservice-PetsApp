package com.MyAnimaLog.Veterinary.application.staff.services;

import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.dto.DomainEvent;
import com.MyAnimaLog.Veterinary.application.shared.dto.EventMetadata;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.EventPublisherPort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.application.staff.dto.TransferOwnershipRequest;
import com.MyAnimaLog.Veterinary.application.staff.dto.TransferOwnershipResponse;
import com.MyAnimaLog.Veterinary.application.staff.ports.in.TransferOwnershipUseCase;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort.ClinicMembership;
import com.MyAnimaLog.Veterinary.domain.shared.enums.VeterinaryEventType;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.EmployeeNotFoundException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.InvalidOwnershipTransferException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.OwnershipTransferConflictException;
import com.MyAnimaLog.Veterinary.domain.staff.model.VeterinaryEmployee;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Transferencia de propiedad de la clínica: el OWNER actual pasa a ADMIN y el destino pasa a OWNER,
 * en una sola transacción.
 *
 * <p><strong>El orden importa</strong>: primero se baja al OWNER actual a ADMIN y después se sube al
 * destino. Al revés, el índice único parcial {@code ux_one_owner_per_vet} (un solo OWNER activo por
 * clínica) rechazaría el {@code UPDATE} por haber dos OWNER a la vez.
 *
 * <p>Los dos pasos son {@code UPDATE} condicionales ({@code claimRoleTransition}, mismo patrón que
 * {@code claimStatusTransition} de la Fase 2B Parte 4B): si cualquiera no actualiza ninguna fila,
 * alguien cambió los roles en el medio y se lanza {@code OwnershipTransferConflictException} (409).
 * Como todo corre dentro de {@code @Transactional}, el fallo del segundo paso revierte el primero:
 * la clínica nunca queda sin OWNER. Entre los dos pasos existe un instante, dentro de la
 * transacción, sin ningún OWNER; eso es correcto y es la única forma de respetar el índice único.
 *
 * <p>El representante legal de la clínica (datos de verificación de la Fase 2B) <strong>no</strong>
 * cambia acá: son cosas distintas (quién manda en la cuenta vs. quién firma legalmente por la
 * clínica). Ver {@code TransferOwnershipResponse.legalRepresentativeChanged} y FASE_3_PERSONAL.md.
 */
@Service
@RequiredArgsConstructor
public class TransferOwnershipService implements TransferOwnershipUseCase {

    private final VeterinaryEmployeeRepositoryPort employeeRepositoryPort;
    private final VeterinaryRepositoryPort veterinaryRepositoryPort;
    private final VeterinaryAuthorizationService authorizationService;
    private final EventPublisherPort eventPublisherPort;

    @Override
    @Transactional
    public TransferOwnershipResponse transfer(UUID veterinaryId, TransferOwnershipRequest request) {
        // Verificado contra la base, y funciona en cualquier estado de la clínica: un dueño tiene
        // que poder entregar la clínica incluso si está suspendida o rechazada.
        ClinicMembership currentOwner = authorizationService.requireVeterinaryOwner(veterinaryId);

        UUID newOwnerEmployeeId = request.getNewOwnerEmployeeId();
        if (newOwnerEmployeeId == null) {
            throw new InvalidOwnershipTransferException("newOwnerEmployeeId is required");
        }
        if (newOwnerEmployeeId.equals(currentOwner.employeeId())) {
            throw new InvalidOwnershipTransferException("That employee is already the OWNER");
        }

        VeterinaryEmployee newOwner = employeeRepositoryPort.findById(newOwnerEmployeeId)
                .orElseThrow(EmployeeNotFoundException::new);
        StaffManagementRules.requireSameClinic(newOwner, veterinaryId, authorizationService);
        if (!Boolean.TRUE.equals(newOwner.getActive())) {
            throw new InvalidOwnershipTransferException("The new OWNER must be an active employee of the clinic");
        }

        // 1. Bajar al OWNER actual (libera el índice único de un solo OWNER por clínica).
        if (!employeeRepositoryPort.claimRoleTransition(
                currentOwner.employeeId(), EmployeeRole.OWNER, EmployeeRole.ADMIN)) {
            throw new OwnershipTransferConflictException();
        }
        // 2. Subir al destino. Si falla, el @Transactional revierte también el paso 1.
        if (!employeeRepositoryPort.claimRoleTransition(
                newOwner.getId(), newOwner.getRole(), EmployeeRole.OWNER)) {
            throw new OwnershipTransferConflictException();
        }

        VeterinaryEmployee previousOwner = employeeRepositoryPort.findById(currentOwner.employeeId())
                .orElseThrow(EmployeeNotFoundException::new);

        publishTransferredEvents(veterinaryId, previousOwner, newOwner);

        return TransferOwnershipResponse.builder()
                .veterinaryId(veterinaryId)
                .previousOwner(TransferOwnershipResponse.Party.builder()
                        .employeeId(previousOwner.getId())
                        .userId(previousOwner.getUserId())
                        .role(EmployeeRole.ADMIN)
                        .build())
                .newOwner(TransferOwnershipResponse.Party.builder()
                        .employeeId(newOwner.getId())
                        .userId(newOwner.getUserId())
                        .role(EmployeeRole.OWNER)
                        .build())
                .legalRepresentativeChanged(false)
                .build();
    }

    /** Un evento a cada parte: el que entrega y el que recibe necesitan enterarse por separado. */
    private void publishTransferredEvents(UUID veterinaryId, VeterinaryEmployee previousOwner,
                                           VeterinaryEmployee newOwner) {
        String veterinaryName = veterinaryRepositoryPort.findById(veterinaryId)
                .map(veterinary -> veterinary.getName())
                .orElse(null);

        publishTransferred(veterinaryId, veterinaryName, newOwner.getUserId(), previousOwner, newOwner, true);
        publishTransferred(veterinaryId, veterinaryName, previousOwner.getUserId(), previousOwner, newOwner, false);
    }

    private void publishTransferred(UUID veterinaryId, String veterinaryName, UUID recipientUserId,
                                     VeterinaryEmployee previousOwner, VeterinaryEmployee newOwner,
                                     boolean recipientIsNewOwner) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("veterinaryName", veterinaryName);
        payload.put("previousOwnerEmployeeId", previousOwner.getId());
        payload.put("newOwnerEmployeeId", newOwner.getId());
        // Permite a Notifications elegir el texto sin comparar ids.
        payload.put("recipientIsNewOwner", recipientIsNewOwner);
        eventPublisherPort.publish(DomainEvent.create(
                VeterinaryEventType.OWNERSHIP_TRANSFERRED,
                recipientUserId,
                null,
                payload,
                EventMetadata.of(veterinaryId, null)));
    }
}

package com.MyAnimaLog.Veterinary.application.staff.services;

import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.professional.ports.out.ProfessionalProfileRepositoryPort;
import com.MyAnimaLog.Veterinary.application.professional.services.LicenseStatusResolver;
import com.MyAnimaLog.Veterinary.application.shared.dto.AuthenticatedUser;
import com.MyAnimaLog.Veterinary.application.shared.dto.DomainEvent;
import com.MyAnimaLog.Veterinary.application.shared.dto.EventMetadata;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.AuthenticatedUserPort;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.EventPublisherPort;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.UserServicePort;
import com.MyAnimaLog.Veterinary.application.staff.dto.AcceptInvitationRequest;
import com.MyAnimaLog.Veterinary.application.staff.dto.AcceptInvitationResponse;
import com.MyAnimaLog.Veterinary.application.staff.ports.in.AcceptInvitationUseCase;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.EmployeeInvitationRepositoryPort;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.ClinicNotActiveException;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.shared.enums.VeterinaryEventType;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VeterinaryNotFoundException;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.domain.staff.enums.InvitationStatus;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.EmployeeAlreadyExistsException;
import com.MyAnimaLog.Veterinary.domain.staff.model.EmployeeInvitation;
import com.MyAnimaLog.Veterinary.domain.staff.model.VeterinaryEmployee;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * El invitado acepta con su <strong>token normal</strong> (el del login), nunca con un token de
 * clínica: todavía no es miembro de ninguna. La identidad sale del token y el único dato que aporta
 * el cuerpo es la invitación en sí, así que no hay nada que un llamador pueda falsificar sobre quién
 * está aceptando.
 */
@Service
@RequiredArgsConstructor
public class AcceptInvitationService implements AcceptInvitationUseCase {

    private final EmployeeInvitationRepositoryPort invitationRepositoryPort;
    private final VeterinaryEmployeeRepositoryPort employeeRepositoryPort;
    private final VeterinaryRepositoryPort veterinaryRepositoryPort;
    private final ProfessionalProfileRepositoryPort professionalProfileRepositoryPort;
    private final AuthenticatedUserPort authenticatedUserPort;
    private final UserServicePort userServicePort;
    private final EventPublisherPort eventPublisherPort;

    @Override
    @Transactional
    public AcceptInvitationResponse accept(AcceptInvitationRequest request) {
        AuthenticatedUser caller = authenticatedUserPort.current();
        LocalDateTime now = LocalDateTime.now();

        EmployeeInvitation invitation = InvitationGate.requireUsableBy(
                invitationRepositoryPort, caller, InvitationGate.hashOf(request.getToken()), now);

        Veterinary veterinary = veterinaryRepositoryPort.findById(invitation.getVeterinaryId())
                .orElseThrow(VeterinaryNotFoundException::new);
        if (veterinary.getStatus() != VeterinaryStatus.ACTIVE) {
            throw new ClinicNotActiveException();
        }

        UpsertedEmployee upserted = upsertEmployee(invitation, caller.userId(), now);
        VeterinaryEmployee employee = upserted.employee();

        invitationRepositoryPort.save(invitation.toBuilder()
                .status(InvitationStatus.ACCEPTED)
                .acceptedUserId(caller.userId())
                .decidedAt(now)
                .build());

        publishJoinedEvent(veterinary, employee, caller);

        return AcceptInvitationResponse.builder()
                .employeeId(employee.getId())
                .veterinaryId(veterinary.getId())
                .veterinaryName(veterinary.getName())
                .role(employee.getRole())
                .licensed(LicenseStatusResolver.isLicensed(professionalProfileRepositoryPort, caller.userId()))
                .reactivated(upserted.reactivated())
                .build();
    }

    private record UpsertedEmployee(VeterinaryEmployee employee, boolean reactivated) {
    }

    /**
     * Si la persona ya trabajó en esta clínica y quedó inactiva, se reactiva su registro con el rol
     * nuevo en vez de insertar otro: el índice único {@code ux_employee_user_per_vet}
     * ({@code veterinary_id, user_id}) no admite dos filas, y además así no se pierde el historial
     * del empleado (su {@code created_at} original y las historias clínicas que ya firmó).
     */
    private UpsertedEmployee upsertEmployee(EmployeeInvitation invitation, UUID userId, LocalDateTime now) {
        Optional<VeterinaryEmployee> existing =
                employeeRepositoryPort.findByVeterinaryIdAndUserId(invitation.getVeterinaryId(), userId);

        if (existing.isPresent()) {
            VeterinaryEmployee employee = existing.get();
            if (Boolean.TRUE.equals(employee.getActive())) {
                throw new EmployeeAlreadyExistsException();
            }
            return new UpsertedEmployee(employeeRepositoryPort.save(employee.toBuilder()
                    .role(invitation.getRole())
                    .active(true)
                    .removedAt(null)
                    .build()), true);
        }

        return new UpsertedEmployee(employeeRepositoryPort.save(VeterinaryEmployee.builder()
                .id(UUID.randomUUID())
                .veterinaryId(invitation.getVeterinaryId())
                .userId(userId)
                .role(invitation.getRole())
                .active(true)
                .createdAt(now)
                .build()), false);
    }

    /** Avisa al OWNER activo de la clínica que alguien entró al equipo. */
    private void publishJoinedEvent(Veterinary veterinary, VeterinaryEmployee employee, AuthenticatedUser caller) {
        employeeRepositoryPort.findByVeterinaryIdAndRoleAndActiveTrue(veterinary.getId(), EmployeeRole.OWNER)
                .ifPresent(owner -> {
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("veterinaryName", veterinary.getName());
                    payload.put("employeeId", employee.getId());
                    payload.put("employeeEmail", caller.email());
                    payload.put("employeeName", resolveName(caller.userId()));
                    payload.put("role", employee.getRole().name());
                    eventPublisherPort.publish(DomainEvent.create(
                            VeterinaryEventType.EMPLOYEE_JOINED,
                            owner.getUserId(),
                            null,
                            payload,
                            EventMetadata.of(veterinary.getId(), null)));
                });
    }

    /** Best-effort: {@code getUsers} nunca lanza, y el nombre puede no venir (ver UserServicePort). */
    private String resolveName(UUID userId) {
        return Optional.ofNullable(userServicePort.getUsers(List.of(userId)).get(userId))
                .map(UserServicePort.UserInfo::name)
                .orElse(null);
    }
}

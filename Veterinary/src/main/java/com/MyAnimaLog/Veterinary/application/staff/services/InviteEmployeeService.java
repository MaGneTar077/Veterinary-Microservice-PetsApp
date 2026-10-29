package com.MyAnimaLog.Veterinary.application.staff.services;

import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.dto.AuthenticatedUser;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.AuthenticatedUserPort;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.UserServicePort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.application.staff.dto.InviteEmployeeRequest;
import com.MyAnimaLog.Veterinary.application.staff.dto.InviteEmployeeResponse;
import com.MyAnimaLog.Veterinary.application.staff.ports.in.InviteEmployeeUseCase;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.EmployeeInvitationRepositoryPort;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.ClinicNotActiveException;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.security.Permission;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VeterinaryNotFoundException;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.domain.staff.enums.InvitationStatus;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.EmployeeAlreadyExistsException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.EmployeeNotFoundException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.InvalidInvitationEmailException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.InvitationAlreadyPendingException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.InvalidEmployeeRoleException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.OnlyOwnerCanManageAdminsException;
import com.MyAnimaLog.Veterinary.domain.staff.model.EmployeeInvitation;
import com.MyAnimaLog.Veterinary.domain.staff.model.VeterinaryEmployee;
import com.MyAnimaLog.Veterinary.infrastructure.security.InvitationTokens;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InviteEmployeeService implements InviteEmployeeUseCase {

    private static final String EMAIL_PATTERN = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$";
    private static final int EXPIRATION_DAYS = 7;

    private final EmployeeInvitationRepositoryPort invitationRepositoryPort;
    private final VeterinaryEmployeeRepositoryPort employeeRepositoryPort;
    private final VeterinaryRepositoryPort veterinaryRepositoryPort;
    private final VeterinaryAuthorizationService authorizationService;
    private final AuthenticatedUserPort authenticatedUserPort;
    private final UserServicePort userServicePort;
    private final InvitationNotifier invitationNotifier;

    @Override
    @Transactional
    public InviteEmployeeResponse invite(UUID veterinaryId, InviteEmployeeRequest request) {
        authorizationService.require(veterinaryId, Permission.STAFF_MANAGE);

        EmployeeRole role = validateRole(request.getRole());
        String email = normalizeEmail(request.getEmail());

        Veterinary veterinary = veterinaryRepositoryPort.findById(veterinaryId)
                .orElseThrow(VeterinaryNotFoundException::new);
        // requireActiveMember solo descarta SUSPENDED; invitar exige además que la clínica ya esté
        // aprobada (una clínica en PENDING_DOCUMENTS conserva STAFF_MANAGE por RolePermissions).
        if (veterinary.getStatus() != VeterinaryStatus.ACTIVE) {
            throw new ClinicNotActiveException();
        }

        AuthenticatedUser caller = authenticatedUserPort.current();
        VeterinaryEmployee actor = employeeRepositoryPort
                .findByVeterinaryIdAndUserId(veterinaryId, caller.userId())
                .orElseThrow(EmployeeNotFoundException::new);
        if (role == EmployeeRole.ADMIN && actor.getRole() != EmployeeRole.OWNER) {
            throw new OnlyOwnerCanManageAdminsException();
        }

        rejectIfAlreadyActiveEmployee(veterinaryId, email);

        LocalDateTime now = LocalDateTime.now();
        revokeExpiredPendingInvitation(veterinaryId, email, now);

        // TODO(VET-40 / Fase 6): cuando el plan tenga max_employees, llamar aquí a
        // PlanLimitGuard.requireCapacity(veterinaryId, Resource.EMPLOYEES) comparando contra
        // countSeatsUsed(...). Hoy VeterinarySubscription no guarda ningún límite (plan es texto
        // libre), así que el conteo se expone en la respuesta pero no bloquea nada.

        String clearToken = InvitationTokens.generate();
        EmployeeInvitation invitation = invitationRepositoryPort.save(EmployeeInvitation.builder()
                .id(UUID.randomUUID())
                .veterinaryId(veterinaryId)
                .email(email)
                .role(role)
                .tokenHash(InvitationTokens.hash(clearToken))
                .status(InvitationStatus.PENDING)
                .invitedBy(caller.userId())
                .expiresAt(now.plusDays(EXPIRATION_DAYS))
                .createdAt(now)
                .build());

        invitationNotifier.publishInvited(invitation, clearToken, veterinary);

        return InviteEmployeeResponse.builder()
                .invitation(InvitationSummaries.of(invitation, now))
                .seatsUsed(countSeatsUsed(veterinaryId, now))
                .build();
    }

    private EmployeeRole validateRole(EmployeeRole role) {
        if (role == null) {
            throw new InvalidEmployeeRoleException();
        }
        if (role == EmployeeRole.OWNER) {
            throw new InvalidEmployeeRoleException(
                    "Cannot invite someone as OWNER; ownership only changes through ownership-transfer");
        }
        return role;
    }

    private String normalizeEmail(String rawEmail) {
        if (rawEmail == null) {
            throw new InvalidInvitationEmailException();
        }
        String email = rawEmail.trim().toLowerCase();
        if (!email.matches(EMAIL_PATTERN)) {
            throw new InvalidInvitationEmailException();
        }
        return email;
    }

    /**
     * Rechaza la invitación si ese correo ya pertenece a un empleado activo de la clínica.
     *
     * <p>Es best-effort por diseño: los empleados se guardan por {@code userId}, no por correo, y el
     * User service no expone una búsqueda por correo todavía, así que hay que resolver los correos de
     * los empleados activos ({@code UserServicePort.getUsers}, que nunca lanza — ver su contrato). Si
     * el User service está caído o no devuelve el correo de alguien, la invitación se permite; la
     * validación autoritativa corre igual al aceptar, que es por {@code userId} y no puede fallar así.
     */
    private void rejectIfAlreadyActiveEmployee(UUID veterinaryId, String email) {
        List<UUID> activeUserIds = employeeRepositoryPort.findByVeterinaryId(veterinaryId).stream()
                .filter(employee -> Boolean.TRUE.equals(employee.getActive()))
                .map(VeterinaryEmployee::getUserId)
                .toList();
        boolean alreadyEmployed = userServicePort.getUsers(activeUserIds).values().stream()
                .map(UserServicePort.UserInfo::email)
                .filter(Objects::nonNull)
                .anyMatch(existing -> existing.trim().equalsIgnoreCase(email));
        if (alreadyEmployed) {
            throw new EmployeeAlreadyExistsException("That email already belongs to an active employee of this veterinary");
        }
    }

    /**
     * Una invitación PENDING vencida sigue ocupando el índice único parcial
     * {@code ux_pending_invitation_per_vet_email}, así que se marca REVOKED antes de crear la nueva
     * (misma transacción). Una PENDING vigente, en cambio, es un conflicto real.
     */
    private void revokeExpiredPendingInvitation(UUID veterinaryId, String email, LocalDateTime now) {
        Optional<EmployeeInvitation> pending = invitationRepositoryPort
                .findByVeterinaryIdAndEmailAndStatus(veterinaryId, email, InvitationStatus.PENDING);
        if (pending.isEmpty()) {
            return;
        }
        EmployeeInvitation existing = pending.get();
        if (!existing.isExpired(now)) {
            throw new InvitationAlreadyPendingException();
        }
        invitationRepositoryPort.save(existing.toBuilder()
                .status(InvitationStatus.REVOKED)
                .decidedAt(now)
                .build());
    }

    /** Empleados activos + invitaciones PENDING vigentes (incluida la que se acaba de crear). */
    private int countSeatsUsed(UUID veterinaryId, LocalDateTime now) {
        long activeEmployees = employeeRepositoryPort.findByVeterinaryId(veterinaryId).stream()
                .filter(employee -> Boolean.TRUE.equals(employee.getActive()))
                .count();
        long pendingInvitations = invitationRepositoryPort
                .findByVeterinaryIdAndStatus(veterinaryId, InvitationStatus.PENDING).stream()
                .filter(invitation -> !invitation.isExpired(now))
                .count();
        return (int) (activeEmployees + pendingInvitations);
    }
}

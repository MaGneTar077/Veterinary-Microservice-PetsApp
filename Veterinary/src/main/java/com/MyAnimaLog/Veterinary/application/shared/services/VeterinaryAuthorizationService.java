package com.MyAnimaLog.Veterinary.application.shared.services;

import com.MyAnimaLog.Veterinary.application.shared.dto.AuthenticatedUser;
import com.MyAnimaLog.Veterinary.application.shared.dto.ClinicContext;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.AuthenticatedUserPort;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort.ClinicMembership;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.security.Permission;
import com.MyAnimaLog.Veterinary.domain.security.RolePermissions;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.ClinicContextRequiredException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.ClinicSuspendedException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.InsufficientPermissionException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.NotClinicMemberException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.NotVeterinaryOwnerException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.PlatformAdminRequiredException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.TenantMismatchException;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Único lugar donde se decide qué puede hacer el caller sobre una clínica.
 *
 * <p><strong>El rol y el estado salen siempre de la base, no del token</strong> (Fase 3): un token
 * de clínica vive hasta 60 min, así que sus claims {@code vet_role}/{@code vet_status} pueden estar
 * obsoletos. Si no fuera así, un empleado al que le bajan el rol o al que retiran seguiría operando
 * con sus permisos viejos hasta que su token expirara. El claim de clínica se usa solo para dos
 * cosas: exigir que el caller esté operando en contexto de clínica y verificar el tenant
 * ({@code vet_id} contra la clínica de la ruta).
 *
 * <p>La única excepción es {@code vet_licensed}, que se sigue tomando del claim: traerlo de la base
 * costaría una lectura más de {@code professional_profiles} y solo <em>agrega</em> permisos clínicos
 * ({@code CLINICAL_WRITE}). Está anotado como deuda técnica conocida en CLAUDE.md.
 *
 * <p>Toda la información viene de una sola consulta
 * ({@link VeterinaryEmployeeRepositoryPort#findClinicMembership}), que reemplazó la lectura de la
 * clínica que antes hacía {@code requireActiveMember} solo para detectar la suspensión.
 */
@Service
@RequiredArgsConstructor
public class VeterinaryAuthorizationService {

    // Ya no depende de VeterinaryRepositoryPort: el estado de la clínica llega en la misma fila que
    // la membresía (findClinicMembership), así que la lectura aparte desapareció.
    private final AuthenticatedUserPort authenticatedUserPort;
    private final VeterinaryEmployeeRepositoryPort employeeRepositoryPort;

    /** For operations that require a specific permission over the clinic in the path. */
    public ClinicContext require(UUID veterinaryIdFromPath, Permission permission) {
        ClinicContext context = requireActiveMember(veterinaryIdFromPath);
        requirePermission(context, permission);
        return context;
    }

    /**
     * For operations that only require being a member of the clinic (any role).
     *
     * <p>No rechaza una clínica {@code SUSPENDED}: para eso está
     * {@link #requireActiveMember(UUID)}, que es el default para cualquier endpoint con token de
     * clínica. Este se usa solo donde la pantalla existe justamente para explicar el estado
     * suspendido ({@code GetVeterinaryProfile}) o donde la acción debe poder hacerse igual sobre una
     * clínica suspendida ({@code LeaveVeterinary}: irse nunca debería depender del estado de la
     * clínica).
     */
    public ClinicContext requireMember(UUID veterinaryIdFromPath) {
        return membershipContext(veterinaryIdFromPath).context();
    }

    /**
     * Same as {@link #requireMember(UUID)} but additionally rejects a {@code SUSPENDED} clinic. El
     * estado viene de la misma fila que la membresía, así que no cuesta una lectura extra.
     */
    public ClinicContext requireActiveMember(UUID veterinaryIdFromPath) {
        Membership membership = membershipContext(veterinaryIdFromPath);
        if (membership.membership().veterinaryStatus() == VeterinaryStatus.SUSPENDED) {
            throw new ClinicSuspendedException();
        }
        return membership.context();
    }

    /**
     * Clínica del token de clínica del caller, si hay. Solo para las rutas viejas de empleados
     * ({@code /api/veterinary/employees/{employeeId}/...}), que no llevan la clínica en la ruta: con
     * esto pueden aplicar el mismo criterio que las nuevas y responder {@code 404} si el empleado
     * objetivo es de otra clínica, en vez de delatar su existencia con un {@code TENANT_MISMATCH}.
     * No autoriza nada por sí solo: el permiso se sigue exigiendo con {@link #require}.
     */
    public Optional<UUID> currentClinicId() {
        return authenticatedUserPort.current().clinic().map(ClinicContext::veterinaryId);
    }

    public void requirePlatformAdmin() {
        AuthenticatedUser user = authenticatedUserPort.current();
        if (!user.platformAdmin()) {
            throw new PlatformAdminRequiredException();
        }
    }

    /**
     * For operations a user can perform on their own record (self-service) or that clinic staff
     * can perform on a user's behalf (e.g. unlinking a client). Allows the call if the caller's
     * own userId matches targetUserId, regardless of clinic context; otherwise requires the
     * caller to hold {@code permission} over the clinic in the path.
     */
    public void requireSelfOrPermission(UUID targetUserId, UUID veterinaryIdFromPath, Permission permission) {
        AuthenticatedUser user = authenticatedUserPort.current();
        if (user.userId().equals(targetUserId)) {
            return;
        }
        // Un caller sin contexto de clínica (o de otra clínica) actuando sobre un tercero no recibe
        // CLINIC_CONTEXT_REQUIRED/TENANT_MISMATCH sino INSUFFICIENT_PERMISSION: desde afuera, no
        // tener el permiso y no tener contexto son el mismo "no podés hacer esto".
        ClinicContext claim = user.clinic().orElseThrow(() -> new InsufficientPermissionException(permission));
        if (!claim.veterinaryId().equals(veterinaryIdFromPath)) {
            throw new InsufficientPermissionException(permission);
        }
        require(veterinaryIdFromPath, permission);
    }

    /**
     * For the clinic registration/verification assistant (D1 in FASE_2B_VERIFICACION.md): accepts
     * either a normal token or a clinic token (if its {@code vet_id} matches), but always verifies
     * against the database that the caller is the clinic's active OWNER employee — never trusts
     * {@code vet_role} alone, since the whole point is that this must also work while the clinic is
     * {@code REJECTED} (no clinic token would even be issued for it). Por lo mismo no mira el estado
     * de la clínica: el dueño tiene que poder arreglar su verificación en cualquier estado.
     */
    public ClinicMembership requireVeterinaryOwner(UUID veterinaryIdFromPath) {
        AuthenticatedUser user = authenticatedUserPort.current();
        requireTenantMatch(user, veterinaryIdFromPath);
        return employeeRepositoryPort.findClinicMembership(veterinaryIdFromPath, user.userId())
                .filter(membership -> membership.role() == EmployeeRole.OWNER)
                .filter(ClinicMembership::active)
                .orElseThrow(NotVeterinaryOwnerException::new);
    }

    /**
     * For operations the OWNER can always perform (even pre-approval) that staff with
     * {@code CLINIC_CONFIGURE} can also perform once the clinic is operating normally (e.g. the
     * clinic logo).
     */
    public void requireOwnerOrPermission(UUID veterinaryIdFromPath, Permission permission) {
        AuthenticatedUser user = authenticatedUserPort.current();
        Optional<ClinicMembership> membership =
                employeeRepositoryPort.findClinicMembership(veterinaryIdFromPath, user.userId());

        if (membership.isPresent()) {
            ClinicMembership row = membership.get();
            if (row.veterinaryStatus() == VeterinaryStatus.SUSPENDED) {
                throw new ClinicSuspendedException();
            }
            boolean actingAsClinicStaff = row.active()
                    && user.clinic().map(claim -> claim.veterinaryId().equals(veterinaryIdFromPath)).orElse(false)
                    && permissionsOf(row, user).contains(permission);
            if (actingAsClinicStaff) {
                return;
            }
            if (row.role() == EmployeeRole.OWNER && row.active()) {
                requireTenantMatch(user, veterinaryIdFromPath);
                return;
            }
        }
        throw new NotVeterinaryOwnerException();
    }

    /**
     * Resuelve la membresía real del caller y la convierte en el {@link ClinicContext} que ven los
     * servicios: {@code employeeId}/{@code role}/{@code status} de la base, {@code licensed} del claim.
     */
    private Membership membershipContext(UUID veterinaryIdFromPath) {
        AuthenticatedUser user = authenticatedUserPort.current();
        ClinicContext claim = user.clinic().orElseThrow(ClinicContextRequiredException::new);
        if (!claim.veterinaryId().equals(veterinaryIdFromPath)) {
            throw new TenantMismatchException();
        }

        ClinicMembership row = employeeRepositoryPort.findClinicMembership(veterinaryIdFromPath, user.userId())
                .filter(ClinicMembership::active)
                .orElseThrow(NotClinicMemberException::new);

        return new Membership(row, new ClinicContext(
                veterinaryIdFromPath,
                row.employeeId(),
                row.role(),
                claim.licensed(),
                row.veterinaryStatus()));
    }

    private void requirePermission(ClinicContext context, Permission permission) {
        if (!RolePermissions.resolve(context.role(), context.licensed(), context.status()).contains(permission)) {
            throw new InsufficientPermissionException(permission);
        }
    }

    private Set<Permission> permissionsOf(ClinicMembership row, AuthenticatedUser user) {
        boolean licensed = user.clinic().map(ClinicContext::licensed).orElse(false);
        return RolePermissions.resolve(row.role(), licensed, row.veterinaryStatus());
    }

    private void requireTenantMatch(AuthenticatedUser user, UUID veterinaryIdFromPath) {
        if (user.clinic().isPresent() && !user.clinic().get().veterinaryId().equals(veterinaryIdFromPath)) {
            throw new TenantMismatchException();
        }
    }

    private record Membership(ClinicMembership membership, ClinicContext context) {
    }
}

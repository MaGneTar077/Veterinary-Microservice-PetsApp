package com.MyAnimaLog.Veterinary.application.shared.services;

import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.dto.AuthenticatedUser;
import com.MyAnimaLog.Veterinary.application.shared.dto.ClinicContext;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.AuthenticatedUserPort;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.security.Permission;
import com.MyAnimaLog.Veterinary.domain.security.RolePermissions;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.ClinicContextRequiredException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.ClinicSuspendedException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.InsufficientPermissionException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.NotVeterinaryOwnerException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.PlatformAdminRequiredException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.TenantMismatchException;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.domain.staff.model.VeterinaryEmployee;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VeterinaryAuthorizationService {

    private final AuthenticatedUserPort authenticatedUserPort;
    private final VeterinaryEmployeeRepositoryPort employeeRepositoryPort;
    private final VeterinaryRepositoryPort veterinaryRepositoryPort;

    /** For operations that require a specific permission over the clinic in the path. */
    public ClinicContext require(UUID veterinaryIdFromPath, Permission permission) {
        ClinicContext context = requireActiveMember(veterinaryIdFromPath);
        if (!RolePermissions.resolve(context.role(), context.licensed(), context.status()).contains(permission)) {
            throw new InsufficientPermissionException(permission);
        }
        return context;
    }

    /** For operations that only require being a member of the clinic (any role). */
    public ClinicContext requireMember(UUID veterinaryIdFromPath) {
        AuthenticatedUser user = authenticatedUserPort.current();
        ClinicContext context = user.clinic().orElseThrow(ClinicContextRequiredException::new);
        if (!context.veterinaryId().equals(veterinaryIdFromPath)) {
            throw new TenantMismatchException();
        }
        return context;
    }

    /**
     * Same as {@link #requireMember(UUID)} but additionally rejects a {@code SUSPENDED} clinic — the
     * clinic token's own {@code vet_status} claim can be up to 60 min stale, so this re-checks the
     * database. Used by every clinic-token endpoint except the clinic status/profile screen itself
     * (which must keep working while suspended so the frontend can show why).
     */
    public ClinicContext requireActiveMember(UUID veterinaryIdFromPath) {
        ClinicContext context = requireMember(veterinaryIdFromPath);
        rejectIfSuspended(veterinaryIdFromPath);
        return context;
    }

    private void rejectIfSuspended(UUID veterinaryIdFromPath) {
        veterinaryRepositoryPort.findById(veterinaryIdFromPath)
                .filter(v -> v.getStatus() == VeterinaryStatus.SUSPENDED)
                .ifPresent(v -> {
                    throw new ClinicSuspendedException();
                });
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
        ClinicContext context = user.clinic().orElseThrow(() -> new InsufficientPermissionException(permission));
        if (!context.veterinaryId().equals(veterinaryIdFromPath)) {
            throw new InsufficientPermissionException(permission);
        }
        if (!RolePermissions.resolve(context.role(), context.licensed(), context.status()).contains(permission)) {
            throw new InsufficientPermissionException(permission);
        }
        rejectIfSuspended(veterinaryIdFromPath);
    }

    /**
     * For the clinic registration/verification assistant (D1 in FASE_2B_VERIFICACION.md): accepts
     * either a normal token or a clinic token (if its {@code vet_id} matches), but always verifies
     * against the database that the caller is the clinic's active OWNER employee — never trusts
     * {@code vet_role} alone, since the whole point is that this must also work while the clinic is
     * {@code REJECTED} (no clinic token would even be issued for it).
     */
    public void requireVeterinaryOwner(UUID veterinaryIdFromPath) {
        AuthenticatedUser user = authenticatedUserPort.current();
        if (user.clinic().isPresent() && !user.clinic().get().veterinaryId().equals(veterinaryIdFromPath)) {
            throw new TenantMismatchException();
        }
        boolean isOwner = employeeRepositoryPort.findByVeterinaryIdAndUserId(veterinaryIdFromPath, user.userId())
                .filter(employee -> employee.getRole() == EmployeeRole.OWNER)
                .filter(employee -> Boolean.TRUE.equals(employee.getActive()))
                .isPresent();
        if (!isOwner) {
            throw new NotVeterinaryOwnerException();
        }
    }

    /**
     * For operations the OWNER can always perform (even pre-approval) that staff with
     * {@code CLINIC_CONFIGURE} can also perform once the clinic is operating normally (e.g. the
     * clinic logo).
     */
    public void requireOwnerOrPermission(UUID veterinaryIdFromPath, Permission permission) {
        rejectIfSuspended(veterinaryIdFromPath);
        AuthenticatedUser user = authenticatedUserPort.current();
        Optional<ClinicContext> clinic = user.clinic();
        if (clinic.isPresent()
                && clinic.get().veterinaryId().equals(veterinaryIdFromPath)
                && RolePermissions.resolve(clinic.get().role(), clinic.get().licensed(), clinic.get().status()).contains(permission)) {
            return;
        }
        requireVeterinaryOwner(veterinaryIdFromPath);
    }
}

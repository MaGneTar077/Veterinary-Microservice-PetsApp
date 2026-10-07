package com.MyAnimaLog.Veterinary.application.shared.services;

import com.MyAnimaLog.Veterinary.application.shared.dto.AuthenticatedUser;
import com.MyAnimaLog.Veterinary.application.shared.dto.ClinicContext;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.AuthenticatedUserPort;
import com.MyAnimaLog.Veterinary.domain.security.Permission;
import com.MyAnimaLog.Veterinary.domain.security.RolePermissions;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.ClinicContextRequiredException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.InsufficientPermissionException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.PlatformAdminRequiredException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.TenantMismatchException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VeterinaryAuthorizationService {

    private final AuthenticatedUserPort authenticatedUserPort;

    /** For operations that require a specific permission over the clinic in the path. */
    public ClinicContext require(UUID veterinaryIdFromPath, Permission permission) {
        ClinicContext context = requireMember(veterinaryIdFromPath);
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
    }
}

package com.MyAnimaLog.Veterinary.domain.security;

import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;

import java.util.EnumSet;
import java.util.Set;

/**
 * Implementa la matriz rol -> permisos de CONTRATOS_COMPARTIDOS.md §3.2 para los 5 roles
 * de {@link EmployeeRole}. OWNER hereda todo lo de ADMIN más SUBSCRIPTION_MANAGE.
 */
public final class RolePermissions {

    private static final Set<Permission> PRE_APPROVAL =
            EnumSet.of(Permission.CLINIC_CONFIGURE, Permission.STAFF_MANAGE, Permission.SUBSCRIPTION_MANAGE);

    private RolePermissions() {
    }

    public static Set<Permission> resolve(EmployeeRole role, boolean licensed, VeterinaryStatus status) {
        Set<Permission> permissions = EnumSet.copyOf(base(role));

        // La licencia solo agrega permisos; nunca quita los que el rol ya tiene en base().
        if (licensed && (role == EmployeeRole.OWNER || role == EmployeeRole.ADMIN)) {
            permissions.add(Permission.CLINICAL_WRITE);
            permissions.add(Permission.NURSING_WRITE);
        }
        if (licensed && role == EmployeeRole.VETERINARIAN) {
            permissions.add(Permission.CLINICAL_WRITE);
        }

        if (status != VeterinaryStatus.ACTIVE) {
            permissions.retainAll(PRE_APPROVAL);
        }
        return permissions;
    }

    private static Set<Permission> base(EmployeeRole role) {
        switch (role) {
            case OWNER:
                // Todo lo de ADMIN más SUBSCRIPTION_MANAGE.
                return EnumSet.of(
                        Permission.CLINIC_CONFIGURE,
                        Permission.SUBSCRIPTION_MANAGE,
                        Permission.STAFF_MANAGE,
                        Permission.APPOINTMENT_MANAGE,
                        Permission.PATIENT_REGISTER,
                        Permission.CLINICAL_READ,
                        Permission.CLINICAL_READ_BASIC,
                        Permission.DOCUMENT_UPLOAD,
                        Permission.REPORTS_VIEW
                );
            case ADMIN:
                // Todo lo de OWNER salvo SUBSCRIPTION_MANAGE. CLINICAL_WRITE y NURSING_WRITE
                // dependen de `licensed` para ADMIN (igual que para OWNER en el contrato).
                return EnumSet.of(
                        Permission.CLINIC_CONFIGURE,
                        Permission.STAFF_MANAGE,
                        Permission.APPOINTMENT_MANAGE,
                        Permission.PATIENT_REGISTER,
                        Permission.CLINICAL_READ,
                        Permission.CLINICAL_READ_BASIC,
                        Permission.DOCUMENT_UPLOAD,
                        Permission.REPORTS_VIEW
                );
            case VETERINARIAN:
            case ASSISTANT:
                // Idénticos en el contrato salvo que VETERINARIAN puede sumar CLINICAL_WRITE
                // si está licenciado (ver resolve()); ASSISTANT nunca la tiene.
                return EnumSet.of(
                        Permission.APPOINTMENT_MANAGE,
                        Permission.PATIENT_REGISTER,
                        Permission.CLINICAL_READ,
                        Permission.CLINICAL_READ_BASIC,
                        Permission.NURSING_WRITE,
                        Permission.DOCUMENT_UPLOAD
                );
            case RECEPTIONIST:
                return EnumSet.of(
                        Permission.APPOINTMENT_MANAGE,
                        Permission.PATIENT_REGISTER,
                        Permission.CLINICAL_READ_BASIC
                );
            default:
                return EnumSet.noneOf(Permission.class);
        }
    }
}

package com.MyAnimaLog.Veterinary.application.staff.services;

import com.MyAnimaLog.Veterinary.application.shared.dto.ClinicContext;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.CannotModifyOwnerException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.CannotModifyPeerAdminException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.CannotModifySelfException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.EmployeeNotFoundException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.OnlyOwnerCanManageAdminsException;
import com.MyAnimaLog.Veterinary.domain.staff.model.VeterinaryEmployee;

import java.util.UUID;

/**
 * Reglas de quién puede tocar a quién dentro del equipo. Utilidad estática (no es un puerto ni un
 * servicio) compartida por cambiar rol y retirar, para que las dos acciones no puedan divergir.
 *
 * <p>El {@link ClinicContext} que recibe ya trae el rol y el {@code employeeId} <strong>de la
 * base</strong> (ver {@code VeterinaryAuthorizationService}), así que acá no hace falta releer al
 * actor: hasta la Fase 3, cada uno de estos servicios hacía su propio {@code findById} del actor
 * justamente porque el rol venía del claim y no era confiable.
 */
public final class StaffManagementRules {

    private StaffManagementRules() {
    }

    /**
     * El empleado objetivo tiene que pertenecer a la clínica de la ruta. Mismo criterio que con las
     * invitaciones: si no es de esta clínica, la respuesta es "no existe" (404) y no "no podés".
     *
     * <p>{@code veterinaryIdFromPath} puede ser {@code null}: son las rutas viejas
     * {@code /api/veterinary/employees/{employeeId}/...}, que no llevan la clínica en la ruta y
     * quedaron como alias {@code @Deprecated}. En ese caso la clínica efectiva es la del token de
     * clínica del caller, de modo que el alias aplique exactamente el mismo criterio que la ruta
     * nueva: un {@code employeeId} de otra clínica responde {@code 404} y no un
     * {@code TENANT_MISMATCH} que delataría su existencia.
     */
    public static void requireSameClinic(VeterinaryEmployee target, UUID veterinaryIdFromPath,
                                          VeterinaryAuthorizationService authorizationService) {
        UUID effectiveClinicId = veterinaryIdFromPath != null
                ? veterinaryIdFromPath
                : authorizationService.currentClinicId().orElse(null);
        if (effectiveClinicId != null && !effectiveClinicId.equals(target.getVeterinaryId())) {
            throw new EmployeeNotFoundException();
        }
    }

    /**
     * Guardas comunes a cambiar rol y retirar:
     * <ul>
     *   <li>nadie se modifica a sí mismo (ni el rol ni el retiro: para irse está {@code leave});</li>
     *   <li>al OWNER no lo toca nadie — para cambiarlo está la transferencia de propiedad;</li>
     *   <li>un ADMIN no puede modificar a otro ADMIN (solo el OWNER).</li>
     * </ul>
     */
    public static void requireCanManage(ClinicContext actor, VeterinaryEmployee target) {
        if (target.getId().equals(actor.employeeId())) {
            throw new CannotModifySelfException();
        }
        if (target.getRole() == EmployeeRole.OWNER) {
            throw new CannotModifyOwnerException();
        }
        if (actor.role() == EmployeeRole.ADMIN && target.getRole() == EmployeeRole.ADMIN) {
            throw new CannotModifyPeerAdminException();
        }
    }

    /** Crear o sostener un ADMIN es exclusivo del OWNER (misma regla que al invitar). */
    public static void requireCanAssignRole(ClinicContext actor, EmployeeRole newRole) {
        if (newRole == EmployeeRole.ADMIN && actor.role() != EmployeeRole.OWNER) {
            throw new OnlyOwnerCanManageAdminsException();
        }
    }
}

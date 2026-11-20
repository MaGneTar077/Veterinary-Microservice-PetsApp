package com.MyAnimaLog.Veterinary.application.schedule.services;

import com.MyAnimaLog.Veterinary.application.shared.dto.ClinicContext;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.security.Permission;
import com.MyAnimaLog.Veterinary.domain.security.RolePermissions;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.InsufficientPermissionException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.EmployeeNotFoundException;
import com.MyAnimaLog.Veterinary.domain.staff.model.VeterinaryEmployee;

import java.util.UUID;

/**
 * Guardas compartidas por los casos de uso de horarios y bloqueos. Utilidad estática (mismo criterio
 * que {@code StaffManagementRules} en el módulo {@code staff}).
 */
final class ScheduleGuards {

    private ScheduleGuards() {
    }

    /**
     * El empleado objetivo tiene que ser <strong>activo y de la clínica de la ruta</strong>. Si no,
     * {@code 404} y no {@code 403}: un {@code employeeId} de otra clínica no se delata.
     */
    static UUID requireActiveEmployeeOfClinic(VeterinaryEmployeeRepositoryPort employeeRepositoryPort,
                                               UUID veterinaryId, UUID employeeId) {
        if (employeeId == null) {
            throw new EmployeeNotFoundException();
        }
        return employeeRepositoryPort.findById(employeeId)
                .filter(employee -> veterinaryId.equals(employee.getVeterinaryId()))
                .filter(employee -> Boolean.TRUE.equals(employee.getActive()))
                .map(VeterinaryEmployee::getId)
                .orElseThrow(EmployeeNotFoundException::new);
    }

    /**
     * Chequea {@code CLINIC_CONFIGURE} sobre el {@link ClinicContext} ya resuelto desde la base, sin
     * una consulta extra: el contexto que devuelve {@code requireActiveMember} ya trae el rol y el
     * estado reales (no los del claim).
     */
    static void requireClinicConfigure(ClinicContext context) {
        if (!RolePermissions.resolve(context.role(), context.licensed(), context.status())
                .contains(Permission.CLINIC_CONFIGURE)) {
            throw new InsufficientPermissionException(Permission.CLINIC_CONFIGURE);
        }
    }
}

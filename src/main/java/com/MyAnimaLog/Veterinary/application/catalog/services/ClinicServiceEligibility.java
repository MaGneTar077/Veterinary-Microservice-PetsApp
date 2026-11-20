package com.MyAnimaLog.Veterinary.application.catalog.services;

import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort.EmployeeLicense;
import com.MyAnimaLog.Veterinary.domain.catalog.model.ClinicService;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;

import java.util.List;
import java.util.UUID;

/**
 * Calcula {@code employeeIds} para un servicio del catálogo: empleados activos de la clínica cuyo
 * rol está en {@code allowedRoles} y, si {@code requiresLicense}, licenciados. Utilidad estática
 * (no un puerto ni un servicio) para que ningún caso de uso tenga que copiar este cruce — lo reusan
 * tanto el endpoint interno de un servicio como el de la lista completa.
 */
public final class ClinicServiceEligibility {

    private ClinicServiceEligibility() {
    }

    public static List<UUID> resolveEmployeeIds(ClinicService service, List<EmployeeLicense> activeEmployees) {
        List<EmployeeRole> allowedRoles = service.getAllowedRoles();
        boolean requiresLicense = Boolean.TRUE.equals(service.getRequiresLicense());

        return activeEmployees.stream()
                .filter(employee -> allowedRoles.contains(employee.role()))
                .filter(employee -> !requiresLicense || employee.licensed())
                .map(EmployeeLicense::employeeId)
                .toList();
    }
}

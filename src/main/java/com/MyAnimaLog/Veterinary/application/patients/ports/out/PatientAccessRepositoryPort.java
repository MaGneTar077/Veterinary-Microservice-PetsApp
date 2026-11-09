package com.MyAnimaLog.Veterinary.application.patients.ports.out;

import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.patients.enums.ConsentStatus;
import com.MyAnimaLog.Veterinary.domain.professional.enums.LicenseStatus;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;

import java.util.Optional;
import java.util.UUID;

public interface PatientAccessRepositoryPort {

    /**
     * Toda la información que necesita el endpoint interno {@code access}, en una sola consulta.
     * {@code Optional.empty()} significa que la clínica no existe. Si existe, los campos de
     * empleado/licencia/consentimiento vienen en {@code null} cuando no aplican (no es empleado,
     * no tiene perfil profesional, la mascota nunca se compartió).
     */
    Optional<AccessRow> findAccess(UUID veterinaryId, UUID petId, UUID userId);

    record AccessRow(VeterinaryStatus veterinaryStatus, UUID employeeId, EmployeeRole role,
                      Boolean employeeActive, LicenseStatus licenseStatus,
                      ConsentStatus petConsentStatus) {
    }
}

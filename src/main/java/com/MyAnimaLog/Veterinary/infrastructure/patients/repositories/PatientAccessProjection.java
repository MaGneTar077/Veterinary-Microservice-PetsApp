package com.MyAnimaLog.Veterinary.infrastructure.patients.repositories;

import com.MyAnimaLog.Veterinary.domain.patients.enums.ConsentStatus;
import com.MyAnimaLog.Veterinary.domain.professional.enums.LicenseStatus;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;

import java.util.UUID;

/**
 * Proyección del único query que responde el endpoint interno {@code access}
 * (CONTRATOS_COMPARTIDOS.md §3) — el más llamado del ecosistema. Une
 * {@code veterinary} × {@code veterinary_employee} × {@code professional_profiles} ×
 * {@code veterinary_patients} con {@code LEFT JOIN}: solo {@code veterinary} es obligatorio (si no
 * existe, el {@code Optional} viene vacío); las otras tres pueden no tener fila sin que la consulta
 * falle, y eso es justamente lo que informan sus campos en {@code null}.
 */
public interface PatientAccessProjection {

    String getVeterinaryStatus();

    /** {@code null} si el usuario no es empleado de esta clínica. */
    UUID getEmployeeId();

    /** {@code null} si el usuario no es empleado de esta clínica. */
    EmployeeRole getRole();

    /** {@code null} si el usuario no es empleado; {@code false} si lo retiraron. */
    Boolean getEmployeeActive();

    /** {@code null} si el usuario no tiene {@code ProfessionalProfile}. */
    LicenseStatus getLicenseStatus();

    /** {@code null} si la mascota nunca se compartió con esta clínica. */
    ConsentStatus getPetConsentStatus();
}

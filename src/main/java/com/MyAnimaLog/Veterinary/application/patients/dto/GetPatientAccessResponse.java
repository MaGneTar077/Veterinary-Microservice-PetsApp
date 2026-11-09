package com.MyAnimaLog.Veterinary.application.patients.dto;

import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.security.Permission;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Contrato exacto de CONTRATOS_COMPARTIDOS.md §3 / VETERINARY_REQUERIMIENTOS_DESDE_MEDICAL.md §3.1.
 * {@code hasAccess = false} nunca es un error: clínica/mascota/usuario inexistentes o sin acceso
 * siempre responden {@code 200} con esto, nunca {@code 404}.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GetPatientAccessResponse {
    private boolean hasAccess;
    private VeterinaryStatus veterinaryStatus;
    private UUID employeeId;
    private EmployeeRole role;
    private boolean licensed;
    private List<Permission> permissions;
}

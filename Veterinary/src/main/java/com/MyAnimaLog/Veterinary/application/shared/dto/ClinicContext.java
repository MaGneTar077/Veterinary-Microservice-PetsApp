package com.MyAnimaLog.Veterinary.application.shared.dto;

import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;

import java.util.UUID;

public record ClinicContext(
        UUID veterinaryId,
        UUID employeeId,
        EmployeeRole role,
        boolean licensed,
        VeterinaryStatus status
) {
}

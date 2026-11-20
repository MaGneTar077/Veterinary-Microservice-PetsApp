package com.MyAnimaLog.Veterinary.infrastructure.staff.repositories;

import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;

import java.util.UUID;

public interface EmployeeLicenseProjection {
    UUID getEmployeeId();
    EmployeeRole getRole();
    Boolean getLicensed();
}

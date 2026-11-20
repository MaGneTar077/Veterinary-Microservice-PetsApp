package com.MyAnimaLog.Veterinary.infrastructure.staff.repositories;

import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;

import java.util.UUID;

public interface EmployeeWithLicenseProjection {
    UUID getEmployeeId();
    UUID getUserId();
    EmployeeRole getRole();
    Boolean getActive();
    Boolean getLicensed();
}

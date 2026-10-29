package com.MyAnimaLog.Veterinary.infrastructure.staff.repositories;

import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;

import java.util.UUID;

/**
 * Proyección del join {@code veterinary_employee} × {@code veterinary} que usa
 * {@code findClinicMembership}. {@code veterinaryStatus} llega como {@code String} porque la columna
 * está mapeada así en {@code VeterinaryEntity}; el adaptador la convierte a {@code VeterinaryStatus}.
 */
public interface ClinicMembershipProjection {

    UUID getEmployeeId();

    EmployeeRole getRole();

    Boolean getActive();

    String getVeterinaryStatus();
}

package com.MyAnimaLog.Veterinary.application.schedule.ports.out;

import com.MyAnimaLog.Veterinary.domain.schedule.model.EmployeeSchedule;

import java.util.List;
import java.util.UUID;

public interface EmployeeScheduleRepositoryPort {

    List<EmployeeSchedule> findByVeterinaryIdAndEmployeeId(UUID veterinaryId, UUID employeeId);

    /** Todas las filas de la clínica en una sola consulta, para {@code schedule-rules}. */
    List<EmployeeSchedule> findByVeterinaryId(UUID veterinaryId);

    void deleteByVeterinaryIdAndEmployeeId(UUID veterinaryId, UUID employeeId);

    List<EmployeeSchedule> saveAll(List<EmployeeSchedule> schedules);
}

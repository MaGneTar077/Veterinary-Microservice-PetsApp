package com.MyAnimaLog.Veterinary.infrastructure.schedule.repositories;

import com.MyAnimaLog.Veterinary.infrastructure.schedule.entity.EmployeeScheduleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EmployeeScheduleJpaRepository extends JpaRepository<EmployeeScheduleEntity, UUID> {

    List<EmployeeScheduleEntity> findByVeterinaryIdAndEmployeeId(UUID veterinaryId, UUID employeeId);

    List<EmployeeScheduleEntity> findByVeterinaryId(UUID veterinaryId);

    void deleteByVeterinaryIdAndEmployeeId(UUID veterinaryId, UUID employeeId);
}

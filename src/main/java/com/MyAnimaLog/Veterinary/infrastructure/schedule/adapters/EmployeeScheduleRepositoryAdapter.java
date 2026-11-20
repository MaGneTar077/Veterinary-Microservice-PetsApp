package com.MyAnimaLog.Veterinary.infrastructure.schedule.adapters;

import com.MyAnimaLog.Veterinary.application.schedule.ports.out.EmployeeScheduleRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.schedule.model.EmployeeSchedule;
import com.MyAnimaLog.Veterinary.infrastructure.schedule.mapper.EmployeeScheduleMapper;
import com.MyAnimaLog.Veterinary.infrastructure.schedule.repositories.EmployeeScheduleJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class EmployeeScheduleRepositoryAdapter implements EmployeeScheduleRepositoryPort {

    private final EmployeeScheduleJpaRepository jpaRepository;
    private final EmployeeScheduleMapper mapper;

    @Override
    public List<EmployeeSchedule> findByVeterinaryIdAndEmployeeId(UUID veterinaryId, UUID employeeId) {
        return jpaRepository.findByVeterinaryIdAndEmployeeId(veterinaryId, employeeId)
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<EmployeeSchedule> findByVeterinaryId(UUID veterinaryId) {
        return jpaRepository.findByVeterinaryId(veterinaryId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public void deleteByVeterinaryIdAndEmployeeId(UUID veterinaryId, UUID employeeId) {
        jpaRepository.deleteByVeterinaryIdAndEmployeeId(veterinaryId, employeeId);
    }

    @Override
    public List<EmployeeSchedule> saveAll(List<EmployeeSchedule> schedules) {
        return jpaRepository.saveAll(schedules.stream().map(mapper::toEntity).toList())
                .stream().map(mapper::toDomain).toList();
    }
}

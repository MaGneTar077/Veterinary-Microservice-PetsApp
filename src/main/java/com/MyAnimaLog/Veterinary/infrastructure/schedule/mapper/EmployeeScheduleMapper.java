package com.MyAnimaLog.Veterinary.infrastructure.schedule.mapper;

import com.MyAnimaLog.Veterinary.domain.schedule.model.EmployeeSchedule;
import com.MyAnimaLog.Veterinary.infrastructure.schedule.entity.EmployeeScheduleEntity;
import org.springframework.stereotype.Component;

@Component
public class EmployeeScheduleMapper {

    public EmployeeScheduleEntity toEntity(EmployeeSchedule domain) {
        return EmployeeScheduleEntity.builder()
                .id(domain.getId())
                .veterinaryId(domain.getVeterinaryId())
                .employeeId(domain.getEmployeeId())
                .dayOfWeek(domain.getDayOfWeek().shortValue())
                .startsAt(domain.getStartsAt())
                .endsAt(domain.getEndsAt())
                .build();
    }

    public EmployeeSchedule toDomain(EmployeeScheduleEntity entity) {
        return EmployeeSchedule.builder()
                .id(entity.getId())
                .veterinaryId(entity.getVeterinaryId())
                .employeeId(entity.getEmployeeId())
                .dayOfWeek(entity.getDayOfWeek().intValue())
                .startsAt(entity.getStartsAt())
                .endsAt(entity.getEndsAt())
                .build();
    }
}

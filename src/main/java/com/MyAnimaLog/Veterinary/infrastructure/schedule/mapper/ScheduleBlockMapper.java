package com.MyAnimaLog.Veterinary.infrastructure.schedule.mapper;

import com.MyAnimaLog.Veterinary.domain.schedule.model.ScheduleBlock;
import com.MyAnimaLog.Veterinary.infrastructure.schedule.entity.ScheduleBlockEntity;
import org.springframework.stereotype.Component;

@Component
public class ScheduleBlockMapper {

    public ScheduleBlockEntity toEntity(ScheduleBlock domain) {
        return ScheduleBlockEntity.builder()
                .id(domain.getId())
                .veterinaryId(domain.getVeterinaryId())
                .employeeId(domain.getEmployeeId())
                .startsAt(domain.getStartsAt())
                .endsAt(domain.getEndsAt())
                .reason(domain.getReason())
                .build();
    }

    public ScheduleBlock toDomain(ScheduleBlockEntity entity) {
        return ScheduleBlock.builder()
                .id(entity.getId())
                .veterinaryId(entity.getVeterinaryId())
                .employeeId(entity.getEmployeeId())
                .startsAt(entity.getStartsAt())
                .endsAt(entity.getEndsAt())
                .reason(entity.getReason())
                .build();
    }
}

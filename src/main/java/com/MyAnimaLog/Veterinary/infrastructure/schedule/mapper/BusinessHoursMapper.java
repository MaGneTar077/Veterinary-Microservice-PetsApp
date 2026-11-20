package com.MyAnimaLog.Veterinary.infrastructure.schedule.mapper;

import com.MyAnimaLog.Veterinary.domain.schedule.model.BusinessHours;
import com.MyAnimaLog.Veterinary.infrastructure.schedule.entity.BusinessHoursEntity;
import org.springframework.stereotype.Component;

@Component
public class BusinessHoursMapper {

    public BusinessHoursEntity toEntity(BusinessHours domain) {
        return BusinessHoursEntity.builder()
                .id(domain.getId())
                .veterinaryId(domain.getVeterinaryId())
                .dayOfWeek(domain.getDayOfWeek().shortValue())
                .opensAt(domain.getOpensAt())
                .closesAt(domain.getClosesAt())
                .build();
    }

    public BusinessHours toDomain(BusinessHoursEntity entity) {
        return BusinessHours.builder()
                .id(entity.getId())
                .veterinaryId(entity.getVeterinaryId())
                .dayOfWeek(entity.getDayOfWeek().intValue())
                .opensAt(entity.getOpensAt())
                .closesAt(entity.getClosesAt())
                .build();
    }
}

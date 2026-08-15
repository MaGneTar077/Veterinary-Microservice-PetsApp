package com.MyAnimaLog.Veterinary.infrastructure.mapper;

import com.MyAnimaLog.Veterinary.domain.model.VeterinaryEmployee;
import com.MyAnimaLog.Veterinary.infrastructure.entity.VeterinaryEmployeeEntity;
import org.springframework.stereotype.Component;

@Component
public class VeterinaryEmployeeMapper {

    public VeterinaryEmployeeEntity toEntity(VeterinaryEmployee domain) {
        return VeterinaryEmployeeEntity.builder()
                .id(domain.getId())
                .veterinaryId(domain.getVeterinaryId())
                .userId(domain.getUserId())
                .role(domain.getRole())
                .active(domain.getActive())
                .createdAt(domain.getCreatedAt())
                .build();
    }

    public VeterinaryEmployee toDomain(VeterinaryEmployeeEntity entity) {
        return VeterinaryEmployee.builder()
                .id(entity.getId())
                .veterinaryId(entity.getVeterinaryId())
                .userId(entity.getUserId())
                .role(entity.getRole())
                .active(entity.getActive())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
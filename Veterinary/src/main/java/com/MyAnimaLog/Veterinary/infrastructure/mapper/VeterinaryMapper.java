package com.MyAnimaLog.Veterinary.infrastructure.mapper;

import com.MyAnimaLog.Veterinary.domain.model.Veterinary;
import com.MyAnimaLog.Veterinary.infrastructure.entity.VeterinaryEntity;
import org.springframework.stereotype.Component;

@Component
public class VeterinaryMapper {
    public VeterinaryEntity toEntity(Veterinary domain) {
        return VeterinaryEntity.builder()
                .id(domain.getId())
                .name(domain.getName())
                .city(domain.getCity())
                .phone(domain.getPhone())
                .email(domain.getEmail())
                .inviteCode(domain.getInviteCode())
                .inviteLink(domain.getInviteLink())
                .tenantId(domain.getTenantId())
                .active(domain.getActive())
                .createdAt(domain.getCreatedAt())
                .build();
    }

    public Veterinary toDomain(VeterinaryEntity entity) {
        return Veterinary.builder()
                .id(entity.getId())
                .name(entity.getName())
                .city(entity.getCity())
                .phone(entity.getPhone())
                .email(entity.getEmail())
                .inviteCode(entity.getInviteCode())
                .inviteLink(entity.getInviteLink())
                .tenantId(entity.getTenantId())
                .active(entity.getActive())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}

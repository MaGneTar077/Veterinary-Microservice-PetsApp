package com.MyAnimaLog.Veterinary.infrastructure.staff.mapper;

import com.MyAnimaLog.Veterinary.domain.staff.model.EmployeeInvitation;
import com.MyAnimaLog.Veterinary.infrastructure.staff.entity.EmployeeInvitationEntity;
import org.springframework.stereotype.Component;

@Component
public class EmployeeInvitationMapper {

    public EmployeeInvitationEntity toEntity(EmployeeInvitation domain) {
        return EmployeeInvitationEntity.builder()
                .id(domain.getId())
                .veterinaryId(domain.getVeterinaryId())
                .email(domain.getEmail())
                .role(domain.getRole())
                .tokenHash(domain.getTokenHash())
                .status(domain.getStatus())
                .invitedBy(domain.getInvitedBy())
                .acceptedUserId(domain.getAcceptedUserId())
                .expiresAt(domain.getExpiresAt())
                .decidedAt(domain.getDecidedAt())
                .createdAt(domain.getCreatedAt())
                .build();
    }

    public EmployeeInvitation toDomain(EmployeeInvitationEntity entity) {
        return EmployeeInvitation.builder()
                .id(entity.getId())
                .veterinaryId(entity.getVeterinaryId())
                .email(entity.getEmail())
                .role(entity.getRole())
                .tokenHash(entity.getTokenHash())
                .status(entity.getStatus())
                .invitedBy(entity.getInvitedBy())
                .acceptedUserId(entity.getAcceptedUserId())
                .expiresAt(entity.getExpiresAt())
                .decidedAt(entity.getDecidedAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}

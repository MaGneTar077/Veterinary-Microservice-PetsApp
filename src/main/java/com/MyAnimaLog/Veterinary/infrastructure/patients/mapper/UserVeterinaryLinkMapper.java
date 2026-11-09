package com.MyAnimaLog.Veterinary.infrastructure.patients.mapper;

import com.MyAnimaLog.Veterinary.domain.patients.model.UserVeterinaryLink;
import com.MyAnimaLog.Veterinary.infrastructure.patients.entity.UserVeterinaryLinkEntity;
import org.springframework.stereotype.Component;

@Component
public class UserVeterinaryLinkMapper {

    public UserVeterinaryLinkEntity toEntity(UserVeterinaryLink domain) {
        return UserVeterinaryLinkEntity.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .veterinaryId(domain.getVeterinaryId())
                .status(domain.getStatus())
                .linkedAt(domain.getLinkedAt())
                .unlinkedAt(domain.getUnlinkedAt())
                .ownerName(domain.getOwnerName())
                .ownerEmail(domain.getOwnerEmail())
                .ownerPhone(domain.getOwnerPhone())
                .build();
    }

    public UserVeterinaryLink toDomain(UserVeterinaryLinkEntity entity) {
        return UserVeterinaryLink.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .veterinaryId(entity.getVeterinaryId())
                .status(entity.getStatus())
                .linkedAt(entity.getLinkedAt())
                .unlinkedAt(entity.getUnlinkedAt())
                .ownerName(entity.getOwnerName())
                .ownerEmail(entity.getOwnerEmail())
                .ownerPhone(entity.getOwnerPhone())
                .build();
    }
}

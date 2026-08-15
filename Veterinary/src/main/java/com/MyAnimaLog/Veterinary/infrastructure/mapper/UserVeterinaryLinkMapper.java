package com.MyAnimaLog.Veterinary.infrastructure.mapper;

import com.MyAnimaLog.Veterinary.domain.model.UserVeterinaryLink;
import com.MyAnimaLog.Veterinary.infrastructure.entity.UserVeterinaryLinkEntity;
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
                .build();
    }

    public UserVeterinaryLink toDomain(UserVeterinaryLinkEntity entity) {
        return UserVeterinaryLink.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .veterinaryId(entity.getVeterinaryId())
                .status(entity.getStatus())
                .linkedAt(entity.getLinkedAt())
                .build();
    }
}
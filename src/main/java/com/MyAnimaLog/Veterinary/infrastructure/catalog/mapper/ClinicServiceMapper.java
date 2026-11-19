package com.MyAnimaLog.Veterinary.infrastructure.catalog.mapper;

import com.MyAnimaLog.Veterinary.domain.catalog.enums.ServiceCategory;
import com.MyAnimaLog.Veterinary.domain.catalog.model.ClinicService;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.infrastructure.catalog.entity.ClinicServiceEntity;
import org.springframework.stereotype.Component;

@Component
public class ClinicServiceMapper {

    /** {@code updatedAt} no se copia: lo genera Hibernate con {@code @UpdateTimestamp}. */
    public ClinicServiceEntity toEntity(ClinicService domain) {
        return ClinicServiceEntity.builder()
                .id(domain.getId())
                .veterinaryId(domain.getVeterinaryId())
                .name(domain.getName())
                .category(domain.getCategory().name())
                .durationMinutes(domain.getDurationMinutes())
                .price(domain.getPrice())
                .bookableOnline(domain.getBookableOnline())
                .active(domain.getActive())
                .requiresLicense(domain.getRequiresLicense())
                .allowedRoles(domain.getAllowedRoles().stream().map(Enum::name).toList())
                .createdAt(domain.getCreatedAt())
                .build();
    }

    public ClinicService toDomain(ClinicServiceEntity entity) {
        return ClinicService.builder()
                .id(entity.getId())
                .veterinaryId(entity.getVeterinaryId())
                .name(entity.getName())
                .category(ServiceCategory.valueOf(entity.getCategory()))
                .durationMinutes(entity.getDurationMinutes())
                .price(entity.getPrice())
                .bookableOnline(entity.getBookableOnline())
                .active(entity.getActive())
                .requiresLicense(entity.getRequiresLicense())
                .allowedRoles(entity.getAllowedRoles().stream().map(EmployeeRole::valueOf).toList())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

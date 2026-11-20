package com.MyAnimaLog.Veterinary.application.catalog.services;

import com.MyAnimaLog.Veterinary.application.catalog.dto.ClinicServiceResponse;
import com.MyAnimaLog.Veterinary.domain.catalog.model.ClinicService;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;

/** Mapeo a la forma pública, compartido por Create/Update/Deactivate para no repetirlo 3 veces. */
final class ClinicServiceResponses {

    private ClinicServiceResponses() {
    }

    static ClinicServiceResponse toResponse(ClinicService service) {
        return ClinicServiceResponse.builder()
                .id(service.getId())
                .veterinaryId(service.getVeterinaryId())
                .name(service.getName())
                .category(service.getCategory().name())
                .durationMinutes(service.getDurationMinutes())
                .price(service.getPrice())
                .bookableOnline(service.getBookableOnline())
                .active(service.getActive())
                .requiresLicense(service.getRequiresLicense())
                .allowedRoles(service.getAllowedRoles().stream().map(EmployeeRole::name).toList())
                .createdAt(service.getCreatedAt())
                .updatedAt(service.getUpdatedAt())
                .build();
    }
}

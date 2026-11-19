package com.MyAnimaLog.Veterinary.application.catalog.services;

import com.MyAnimaLog.Veterinary.application.catalog.dto.InternalClinicServiceResponse;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort.EmployeeLicense;
import com.MyAnimaLog.Veterinary.domain.catalog.model.ClinicService;

import java.util.List;

/** Mapeo a la forma que consume Calendar, compartido por los dos casos de uso internos. */
final class InternalClinicServiceResponses {

    private InternalClinicServiceResponses() {
    }

    static InternalClinicServiceResponse toResponse(ClinicService service, List<EmployeeLicense> activeEmployees) {
        return InternalClinicServiceResponse.builder()
                .id(service.getId())
                .name(service.getName())
                .category(service.getCategory().name())
                .durationMinutes(service.getDurationMinutes())
                .price(service.getPrice())
                .active(service.getActive())
                .bookableOnline(service.getBookableOnline())
                .requiresLicense(service.getRequiresLicense())
                .employeeIds(ClinicServiceEligibility.resolveEmployeeIds(service, activeEmployees))
                .build();
    }
}

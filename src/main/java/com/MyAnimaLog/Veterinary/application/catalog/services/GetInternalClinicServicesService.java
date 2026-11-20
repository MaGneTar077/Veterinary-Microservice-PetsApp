package com.MyAnimaLog.Veterinary.application.catalog.services;

import com.MyAnimaLog.Veterinary.application.catalog.dto.InternalClinicServiceResponse;
import com.MyAnimaLog.Veterinary.application.catalog.ports.in.GetInternalClinicServicesUseCase;
import com.MyAnimaLog.Veterinary.application.catalog.ports.out.ClinicServiceRepositoryPort;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort.EmployeeLicense;
import com.MyAnimaLog.Veterinary.domain.catalog.model.ClinicService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/** Lo consume Calendar (CALENDAR_REQUESTS_TO_VETERINARY.md, pedido 2). */
@Service
@RequiredArgsConstructor
public class GetInternalClinicServicesService implements GetInternalClinicServicesUseCase {

    private final ClinicServiceRepositoryPort repositoryPort;
    private final VeterinaryEmployeeRepositoryPort employeeRepositoryPort;

    @Override
    public List<InternalClinicServiceResponse> list(UUID veterinaryId, boolean activeOnly,
                                                      String bookableOnlineFilter) {
        Boolean bookableOnline = ClinicServiceValidation.validateBookableOnlineFilter(bookableOnlineFilter);

        List<ClinicService> services = activeOnly
                ? repositoryPort.findByVeterinaryIdAndActiveTrue(veterinaryId)
                : repositoryPort.findByVeterinaryId(veterinaryId);

        if (bookableOnline != null) {
            services = services.stream()
                    .filter(service -> bookableOnline.equals(service.getBookableOnline()))
                    .toList();
        }

        List<EmployeeLicense> activeEmployees = employeeRepositoryPort.findActiveWithLicense(veterinaryId);

        return services.stream()
                .map(service -> InternalClinicServiceResponses.toResponse(service, activeEmployees))
                .toList();
    }
}

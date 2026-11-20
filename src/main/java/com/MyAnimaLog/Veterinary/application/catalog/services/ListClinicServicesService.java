package com.MyAnimaLog.Veterinary.application.catalog.services;

import com.MyAnimaLog.Veterinary.application.catalog.dto.ListClinicServicesResponse;
import com.MyAnimaLog.Veterinary.application.catalog.ports.in.ListClinicServicesUseCase;
import com.MyAnimaLog.Veterinary.application.catalog.ports.out.ClinicServiceRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.domain.catalog.model.ClinicService;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** Cualquier miembro activo de la clínica puede ver el catálogo completo (activos e inactivos). */
@Service
@RequiredArgsConstructor
public class ListClinicServicesService implements ListClinicServicesUseCase {

    private final VeterinaryAuthorizationService authorizationService;
    private final ClinicServiceRepositoryPort repositoryPort;

    @Override
    public ListClinicServicesResponse list(UUID veterinaryId) {
        authorizationService.requireActiveMember(veterinaryId);

        return ListClinicServicesResponse.builder()
                .items(repositoryPort.findByVeterinaryId(veterinaryId).stream()
                        .map(this::toItem)
                        .toList())
                .build();
    }

    private ListClinicServicesResponse.Item toItem(ClinicService service) {
        return ListClinicServicesResponse.Item.builder()
                .id(service.getId())
                .name(service.getName())
                .category(service.getCategory().name())
                .durationMinutes(service.getDurationMinutes())
                .price(service.getPrice())
                .bookableOnline(service.getBookableOnline())
                .active(service.getActive())
                .requiresLicense(service.getRequiresLicense())
                .allowedRoles(service.getAllowedRoles().stream().map(EmployeeRole::name).toList())
                .build();
    }
}

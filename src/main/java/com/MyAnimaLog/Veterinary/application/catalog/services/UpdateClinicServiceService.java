package com.MyAnimaLog.Veterinary.application.catalog.services;

import com.MyAnimaLog.Veterinary.application.catalog.dto.ClinicServiceResponse;
import com.MyAnimaLog.Veterinary.application.catalog.dto.UpdateClinicServiceRequest;
import com.MyAnimaLog.Veterinary.application.catalog.ports.in.UpdateClinicServiceUseCase;
import com.MyAnimaLog.Veterinary.application.catalog.ports.out.ClinicServiceRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.domain.catalog.enums.ServiceCategory;
import com.MyAnimaLog.Veterinary.domain.catalog.exceptions.ClinicServiceNameAlreadyExistsException;
import com.MyAnimaLog.Veterinary.domain.catalog.exceptions.ClinicServiceNotFoundException;
import com.MyAnimaLog.Veterinary.domain.catalog.model.ClinicService;
import com.MyAnimaLog.Veterinary.domain.security.Permission;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdateClinicServiceService implements UpdateClinicServiceUseCase {

    private final VeterinaryAuthorizationService authorizationService;
    private final ClinicServiceRepositoryPort repositoryPort;

    @Override
    public ClinicServiceResponse update(UUID veterinaryId, UUID serviceId, UpdateClinicServiceRequest request) {
        authorizationService.require(veterinaryId, Permission.CLINIC_CONFIGURE);

        ClinicService service = repositoryPort.findByVeterinaryIdAndId(veterinaryId, serviceId)
                .orElseThrow(ClinicServiceNotFoundException::new);

        String name = request.getName() != null
                ? ClinicServiceValidation.validateName(request.getName())
                : service.getName();
        // Solo choca con OTRO servicio activo: reenviar el mismo nombre sin cambios no debe 409.
        if (!name.equalsIgnoreCase(service.getName())
                && repositoryPort.existsActiveByVeterinaryIdAndName(veterinaryId, name)) {
            throw new ClinicServiceNameAlreadyExistsException();
        }

        ServiceCategory category = request.getCategory() != null
                ? ClinicServiceValidation.validateCategory(request.getCategory())
                : service.getCategory();
        Integer durationMinutes = request.getDurationMinutes() != null
                ? ClinicServiceValidation.validateDurationMinutes(request.getDurationMinutes())
                : service.getDurationMinutes();
        BigDecimal price = request.getPrice() != null
                ? ClinicServiceValidation.validatePrice(request.getPrice())
                : service.getPrice();
        List<EmployeeRole> allowedRoles = request.getAllowedRoles() != null
                ? ClinicServiceValidation.validateAllowedRoles(request.getAllowedRoles())
                : service.getAllowedRoles();

        ClinicService updated = service.toBuilder()
                .name(name)
                .category(category)
                .durationMinutes(durationMinutes)
                .price(price)
                .bookableOnline(request.getBookableOnline() != null
                        ? request.getBookableOnline() : service.getBookableOnline())
                .requiresLicense(request.getRequiresLicense() != null
                        ? request.getRequiresLicense() : service.getRequiresLicense())
                .allowedRoles(allowedRoles)
                .build();

        return ClinicServiceResponses.toResponse(repositoryPort.save(updated));
    }
}

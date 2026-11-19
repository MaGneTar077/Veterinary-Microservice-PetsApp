package com.MyAnimaLog.Veterinary.application.catalog.services;

import com.MyAnimaLog.Veterinary.application.catalog.dto.ClinicServiceResponse;
import com.MyAnimaLog.Veterinary.application.catalog.dto.CreateClinicServiceRequest;
import com.MyAnimaLog.Veterinary.application.catalog.ports.in.CreateClinicServiceUseCase;
import com.MyAnimaLog.Veterinary.application.catalog.ports.out.ClinicServiceRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.domain.catalog.enums.ServiceCategory;
import com.MyAnimaLog.Veterinary.domain.catalog.exceptions.ClinicServiceNameAlreadyExistsException;
import com.MyAnimaLog.Veterinary.domain.catalog.model.ClinicService;
import com.MyAnimaLog.Veterinary.domain.security.Permission;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateClinicServiceService implements CreateClinicServiceUseCase {

    private final VeterinaryAuthorizationService authorizationService;
    private final ClinicServiceRepositoryPort repositoryPort;

    @Override
    public ClinicServiceResponse create(UUID veterinaryId, CreateClinicServiceRequest request) {
        authorizationService.require(veterinaryId, Permission.CLINIC_CONFIGURE);

        String name = ClinicServiceValidation.validateName(request.getName());
        ServiceCategory category = ClinicServiceValidation.validateCategory(request.getCategory());
        Integer durationMinutes = ClinicServiceValidation.validateDurationMinutes(request.getDurationMinutes());
        BigDecimal price = ClinicServiceValidation.validatePrice(request.getPrice());
        List<EmployeeRole> allowedRoles = ClinicServiceValidation.validateAllowedRoles(request.getAllowedRoles());

        if (repositoryPort.existsActiveByVeterinaryIdAndName(veterinaryId, name)) {
            throw new ClinicServiceNameAlreadyExistsException();
        }

        ClinicService service = ClinicService.builder()
                .id(UUID.randomUUID())
                .veterinaryId(veterinaryId)
                .name(name)
                .category(category)
                .durationMinutes(durationMinutes)
                .price(price)
                .bookableOnline(request.getBookableOnline() != null ? request.getBookableOnline() : true)
                .active(true)
                .requiresLicense(request.getRequiresLicense() != null ? request.getRequiresLicense() : false)
                .allowedRoles(allowedRoles)
                .createdAt(LocalDateTime.now())
                .build();

        return ClinicServiceResponses.toResponse(repositoryPort.save(service));
    }
}

package com.MyAnimaLog.Veterinary.application.catalog.services;

import com.MyAnimaLog.Veterinary.application.catalog.dto.ClinicServiceResponse;
import com.MyAnimaLog.Veterinary.application.catalog.ports.in.DeactivateClinicServiceUseCase;
import com.MyAnimaLog.Veterinary.application.catalog.ports.out.ClinicServiceRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.domain.catalog.exceptions.ClinicServiceNotFoundException;
import com.MyAnimaLog.Veterinary.domain.catalog.model.ClinicService;
import com.MyAnimaLog.Veterinary.domain.security.Permission;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** Nunca borra la fila: puede haber citas (en Calendar) que referencian este servicio por id. */
@Service
@RequiredArgsConstructor
public class DeactivateClinicServiceService implements DeactivateClinicServiceUseCase {

    private final VeterinaryAuthorizationService authorizationService;
    private final ClinicServiceRepositoryPort repositoryPort;

    @Override
    public ClinicServiceResponse deactivate(UUID veterinaryId, UUID serviceId) {
        authorizationService.require(veterinaryId, Permission.CLINIC_CONFIGURE);

        ClinicService service = repositoryPort.findByVeterinaryIdAndId(veterinaryId, serviceId)
                .orElseThrow(ClinicServiceNotFoundException::new);

        ClinicService deactivated = service.toBuilder().active(false).build();

        return ClinicServiceResponses.toResponse(repositoryPort.save(deactivated));
    }
}

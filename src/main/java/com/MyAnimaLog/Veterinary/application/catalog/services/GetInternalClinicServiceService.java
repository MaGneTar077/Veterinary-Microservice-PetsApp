package com.MyAnimaLog.Veterinary.application.catalog.services;

import com.MyAnimaLog.Veterinary.application.catalog.dto.InternalClinicServiceResponse;
import com.MyAnimaLog.Veterinary.application.catalog.ports.in.GetInternalClinicServiceUseCase;
import com.MyAnimaLog.Veterinary.application.catalog.ports.out.ClinicServiceRepositoryPort;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.catalog.exceptions.ClinicServiceNotFoundException;
import com.MyAnimaLog.Veterinary.domain.catalog.model.ClinicService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** Devuelve también servicios inactivos: Calendar los necesita para citas ya creadas sobre ellos. */
@Service
@RequiredArgsConstructor
public class GetInternalClinicServiceService implements GetInternalClinicServiceUseCase {

    private final ClinicServiceRepositoryPort repositoryPort;
    private final VeterinaryEmployeeRepositoryPort employeeRepositoryPort;

    @Override
    public InternalClinicServiceResponse get(UUID veterinaryId, UUID serviceId) {
        ClinicService service = repositoryPort.findByVeterinaryIdAndId(veterinaryId, serviceId)
                .orElseThrow(ClinicServiceNotFoundException::new);

        return InternalClinicServiceResponses.toResponse(
                service, employeeRepositoryPort.findActiveWithLicense(veterinaryId));
    }
}

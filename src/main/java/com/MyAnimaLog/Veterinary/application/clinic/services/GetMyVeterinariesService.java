package com.MyAnimaLog.Veterinary.application.clinic.services;

import com.MyAnimaLog.Veterinary.application.clinic.dto.GetMyVeterinariesResponse;
import com.MyAnimaLog.Veterinary.application.clinic.dto.MyVeterinarySummary;
import com.MyAnimaLog.Veterinary.application.clinic.ports.in.GetMyVeterinariesUseCase;
import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.AuthenticatedUserPort;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.staff.model.VeterinaryEmployee;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetMyVeterinariesService implements GetMyVeterinariesUseCase {

    private final AuthenticatedUserPort authenticatedUserPort;
    private final VeterinaryEmployeeRepositoryPort employeeRepositoryPort;
    private final VeterinaryRepositoryPort veterinaryRepositoryPort;

    @Override
    public GetMyVeterinariesResponse getMyVeterinaries() {
        UUID userId = authenticatedUserPort.current().userId();

        List<VeterinaryEmployee> activeEmployments = employeeRepositoryPort.findByUserIdAndActiveTrue(userId);

        List<MyVeterinarySummary> summaries = activeEmployments.stream()
                .map(employee -> {
                    Optional<Veterinary> veterinary = veterinaryRepositoryPort.findById(employee.getVeterinaryId());
                    return veterinary.map(v -> MyVeterinarySummary.builder()
                            .veterinaryId(v.getId())
                            .name(v.getName())
                            .role(employee.getRole())
                            .status(v.getStatus())
                            .build());
                })
                .flatMap(Optional::stream)
                .toList();

        return GetMyVeterinariesResponse.builder()
                .veterinaries(summaries)
                .build();
    }
}

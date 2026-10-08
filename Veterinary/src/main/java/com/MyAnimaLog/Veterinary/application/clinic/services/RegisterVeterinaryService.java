package com.MyAnimaLog.Veterinary.application.clinic.services;

import com.MyAnimaLog.Veterinary.application.clinic.dto.RegisterVeterinaryRequest;
import com.MyAnimaLog.Veterinary.application.clinic.dto.RegisterVeterinaryResponse;
import com.MyAnimaLog.Veterinary.application.clinic.ports.in.RegisterVeterinaryUseCase;
import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.dto.AuthenticatedUser;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.AuthenticatedUserPort;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.EmailNotVerifiedException;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.InvalidVeterinaryEmailException;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.TooManyOwnedVeterinariesException;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.VeterinaryNitAlreadyExistsException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.InvalidVeterinaryNameException;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.VeterinaryAlreadyExistsException;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.VeterinaryEmailAlreadyExistsException;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Nit;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.domain.staff.model.VeterinaryEmployee;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegisterVeterinaryService implements RegisterVeterinaryUseCase {

    private static final int MAX_OWNED_NON_FINAL_VETERINARIES = 3;

    private final VeterinaryRepositoryPort veterinaryRepositoryPort;
    private final VeterinaryEmployeeRepositoryPort employeeRepositoryPort;
    private final AuthenticatedUserPort authenticatedUserPort;

    @Override
    @Transactional
    public RegisterVeterinaryResponse registerVeterinary(RegisterVeterinaryRequest request) {
        AuthenticatedUser caller = authenticatedUserPort.current();
        if (!caller.emailVerified()) {
            throw new EmailNotVerifiedException();
        }

        if (request.getName() == null || request.getName().isBlank()) {
            throw new InvalidVeterinaryNameException();
        }
        if (request.getEmail() == null || request.getEmail().isBlank()
                || !request.getEmail().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new InvalidVeterinaryEmailException();
        }
        Nit nit = Nit.of(request.getNit());

        if (veterinaryRepositoryPort.existsByName(request.getName())) {
            throw new VeterinaryAlreadyExistsException();
        }
        if (veterinaryRepositoryPort.existsByEmail(request.getEmail())) {
            throw new VeterinaryEmailAlreadyExistsException();
        }
        if (veterinaryRepositoryPort.existsByNit(nit.value())) {
            throw new VeterinaryNitAlreadyExistsException();
        }

        long ownedNonFinalCount = employeeRepositoryPort.findByUserIdAndRole(caller.userId(), EmployeeRole.OWNER)
                .stream()
                .map(employee -> veterinaryRepositoryPort.findById(employee.getVeterinaryId()))
                .flatMap(Optional::stream)
                .filter(owned -> owned.getStatus() != VeterinaryStatus.REJECTED)
                .count();
        if (ownedNonFinalCount >= MAX_OWNED_NON_FINAL_VETERINARIES) {
            throw new TooManyOwnedVeterinariesException();
        }

        Veterinary veterinary = Veterinary.builder()
                .id(UUID.randomUUID())
                .name(request.getName())
                .city(request.getCity())
                .phone(request.getPhone())
                .email(request.getEmail())
                .tenantId(UUID.randomUUID().toString())
                .status(VeterinaryStatus.PENDING_DOCUMENTS)
                .active(VeterinaryStatus.PENDING_DOCUMENTS.impliesActiveFlag())
                .legalName(request.getLegalName())
                .nit(nit)
                .address(request.getAddress())
                .department(request.getDepartment())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .timezone("America/Bogota")
                .currency("COP")
                .defaultAppointmentMinutes(30)
                .allowOnlineBooking(true)
                .bookingRequiresConfirmation(true)
                .cancellationMinHours(4)
                .directoryVisible(true)
                .createdBy(caller.userId())
                .createdAt(LocalDateTime.now())
                .build();

        Veterinary saved = veterinaryRepositoryPort.save(veterinary);

        // TODO(VET-14): ownership transfer will need to move this OWNER row, not just read it.
        VeterinaryEmployee creatorAsOwner = VeterinaryEmployee.builder()
                .id(UUID.randomUUID())
                .veterinaryId(saved.getId())
                .userId(caller.userId())
                .role(EmployeeRole.OWNER)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
        employeeRepositoryPort.save(creatorAsOwner);

        return RegisterVeterinaryResponse.builder()
                .id(saved.getId())
                .name(saved.getName())
                .city(saved.getCity())
                .phone(saved.getPhone())
                .email(saved.getEmail())
                .tenantId(saved.getTenantId())
                .active(saved.getActive())
                .status(saved.getStatus())
                .legalName(saved.getLegalName())
                .nit(saved.getNit() != null ? saved.getNit().value() : null)
                .createdAt(saved.getCreatedAt())
                .build();
    }
}

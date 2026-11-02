package com.MyAnimaLog.Veterinary.application.clinic.services;

import com.MyAnimaLog.Veterinary.application.clinic.dto.RegisterVeterinaryRequest;
import com.MyAnimaLog.Veterinary.application.clinic.dto.RegisterVeterinaryResponse;
import com.MyAnimaLog.Veterinary.application.clinic.ports.in.RegisterVeterinaryUseCase;
import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.legal.ports.out.LegalConsentRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.dto.AuthenticatedUser;
import com.MyAnimaLog.Veterinary.application.shared.dto.DomainEvent;
import com.MyAnimaLog.Veterinary.application.shared.dto.EventMetadata;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.AuthenticatedUserPort;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.EventPublisherPort;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.application.verification.ports.out.VerificationRequestRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.ConsentRequiredException;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.ConsentVersionOutdatedException;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.EmailNotVerifiedException;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.InvalidVeterinaryEmailException;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.InvalidVeterinaryFieldException;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.TooManyOwnedVeterinariesException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.InvalidVeterinaryNameException;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.VeterinaryAlreadyExistsException;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.VeterinaryEmailAlreadyExistsException;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.VeterinaryNitAlreadyExistsException;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Nit;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.legal.enums.ConsentType;
import com.MyAnimaLog.Veterinary.domain.legal.model.LegalConsent;
import com.MyAnimaLog.Veterinary.domain.shared.enums.VeterinaryEventType;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.domain.staff.model.VeterinaryEmployee;
import com.MyAnimaLog.Veterinary.domain.verification.enums.SubjectType;
import com.MyAnimaLog.Veterinary.domain.verification.enums.VerificationRequestStatus;
import com.MyAnimaLog.Veterinary.domain.verification.model.VerificationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegisterVeterinaryService implements RegisterVeterinaryUseCase {

    private static final int MAX_OWNED_NON_FINAL_VETERINARIES = 3;
    private static final String PHONE_PATTERN = "^\\+?\\d{7,15}$";

    private final VeterinaryRepositoryPort veterinaryRepositoryPort;
    private final VeterinaryEmployeeRepositoryPort employeeRepositoryPort;
    private final LegalConsentRepositoryPort legalConsentRepositoryPort;
    private final VerificationRequestRepositoryPort verificationRequestRepositoryPort;
    private final AuthenticatedUserPort authenticatedUserPort;
    private final EventPublisherPort eventPublisherPort;

    @Value("${app.legal.data-policy-version}")
    private String currentDataPolicyVersion;

    @Value("${app.legal.terms-version}")
    private String currentTermsVersion;

    @Override
    @Transactional
    public RegisterVeterinaryResponse registerVeterinary(RegisterVeterinaryRequest request) {
        AuthenticatedUser caller = authenticatedUserPort.current();
        if (!caller.emailVerified()) {
            throw new EmailNotVerifiedException();
        }

        validateConsents(request);
        validateFields(request);
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

        LocalDateTime now = LocalDateTime.now();
        legalConsentRepositoryPort.save(LegalConsent.builder()
                .id(UUID.randomUUID())
                .userId(caller.userId())
                .veterinaryId(saved.getId())
                .consentType(ConsentType.DATA_TREATMENT)
                .version(request.getAcceptedDataPolicyVersion())
                .acceptedAt(now)
                .build());
        legalConsentRepositoryPort.save(LegalConsent.builder()
                .id(UUID.randomUUID())
                .userId(caller.userId())
                .veterinaryId(saved.getId())
                .consentType(ConsentType.TERMS)
                .version(request.getAcceptedTermsVersion())
                .acceptedAt(now)
                .build());

        verificationRequestRepositoryPort.save(VerificationRequest.builder()
                .id(UUID.randomUUID())
                .subjectType(SubjectType.CLINIC)
                .subjectId(saved.getId())
                .status(VerificationRequestStatus.DRAFT)
                .attemptNumber(1)
                .submittedBy(caller.userId())
                .registryChecked(false)
                .createdAt(now)
                .build());

        eventPublisherPort.publish(DomainEvent.create(
                VeterinaryEventType.VETERINARY_REGISTERED,
                caller.userId(),
                null,
                Map.of("veterinaryId", saved.getId(), "name", saved.getName()),
                EventMetadata.of(saved.getId(), null)));

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
                .department(saved.getDepartment())
                .address(saved.getAddress())
                .latitude(saved.getLatitude())
                .longitude(saved.getLongitude())
                .logoUrl(saved.getLogoUrl())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    private void validateConsents(RegisterVeterinaryRequest request) {
        if (isBlank(request.getAcceptedDataPolicyVersion()) || isBlank(request.getAcceptedTermsVersion())) {
            throw new ConsentRequiredException();
        }
        if (!currentDataPolicyVersion.equals(request.getAcceptedDataPolicyVersion())
                || !currentTermsVersion.equals(request.getAcceptedTermsVersion())) {
            throw new ConsentVersionOutdatedException();
        }
    }

    private void validateFields(RegisterVeterinaryRequest request) {
        if (isBlank(request.getName())) {
            throw new InvalidVeterinaryNameException();
        }
        if (isBlank(request.getLegalName())) {
            throw new InvalidVeterinaryFieldException("Legal name is required");
        }
        if (isBlank(request.getCity())) {
            throw new InvalidVeterinaryFieldException("City is required");
        }
        if (isBlank(request.getDepartment())) {
            throw new InvalidVeterinaryFieldException("Department is required");
        }
        if (isBlank(request.getAddress())) {
            throw new InvalidVeterinaryFieldException("Address is required");
        }
        if (isBlank(request.getPhone()) || !request.getPhone().matches(PHONE_PATTERN)) {
            throw new InvalidVeterinaryFieldException("Phone must be 7 to 15 digits, with an optional leading +");
        }
        if (isBlank(request.getEmail()) || !request.getEmail().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new InvalidVeterinaryEmailException();
        }
        validateLocation(request.getLatitude(), request.getLongitude());
    }

    private void validateLocation(BigDecimal latitude, BigDecimal longitude) {
        if (latitude == null && longitude == null) {
            return;
        }
        if (latitude == null || longitude == null) {
            throw new InvalidVeterinaryFieldException("latitude and longitude must be provided together");
        }
        if (latitude.compareTo(BigDecimal.valueOf(-90)) < 0 || latitude.compareTo(BigDecimal.valueOf(90)) > 0) {
            throw new InvalidVeterinaryFieldException("latitude must be between -90 and 90");
        }
        if (longitude.compareTo(BigDecimal.valueOf(-180)) < 0 || longitude.compareTo(BigDecimal.valueOf(180)) > 0) {
            throw new InvalidVeterinaryFieldException("longitude must be between -180 and 180");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

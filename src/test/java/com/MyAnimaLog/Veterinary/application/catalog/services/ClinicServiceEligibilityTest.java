package com.MyAnimaLog.Veterinary.application.catalog.services;

import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort.EmployeeLicense;
import com.MyAnimaLog.Veterinary.domain.catalog.enums.ServiceCategory;
import com.MyAnimaLog.Veterinary.domain.catalog.model.ClinicService;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ClinicServiceEligibilityTest {

    private ClinicService service(List<EmployeeRole> allowedRoles, boolean requiresLicense) {
        return ClinicService.builder()
                .id(UUID.randomUUID())
                .veterinaryId(UUID.randomUUID())
                .name("Consulta general")
                .category(ServiceCategory.CONSULTATION)
                .durationMinutes(30)
                .price(BigDecimal.valueOf(40000))
                .bookableOnline(true)
                .active(true)
                .requiresLicense(requiresLicense)
                .allowedRoles(allowedRoles)
                .build();
    }

    @Test
    void resolveEmployeeIds_shouldExcludeEmployees_whoseRoleIsNotAllowed() {
        UUID vetEmployeeId = UUID.randomUUID();
        UUID receptionistId = UUID.randomUUID();
        ClinicService service = service(List.of(EmployeeRole.VETERINARIAN), false);

        List<UUID> result = ClinicServiceEligibility.resolveEmployeeIds(service, List.of(
                new EmployeeLicense(vetEmployeeId, EmployeeRole.VETERINARIAN, false),
                new EmployeeLicense(receptionistId, EmployeeRole.RECEPTIONIST, false)));

        assertThat(result).containsExactly(vetEmployeeId);
    }

    @Test
    void resolveEmployeeIds_shouldExcludeUnlicensedEmployees_whenRequiresLicenseIsTrue() {
        UUID licensedId = UUID.randomUUID();
        UUID unlicensedId = UUID.randomUUID();
        ClinicService service = service(List.of(EmployeeRole.VETERINARIAN), true);

        List<UUID> result = ClinicServiceEligibility.resolveEmployeeIds(service, List.of(
                new EmployeeLicense(licensedId, EmployeeRole.VETERINARIAN, true),
                new EmployeeLicense(unlicensedId, EmployeeRole.VETERINARIAN, false)));

        assertThat(result).containsExactly(licensedId);
    }

    @Test
    void resolveEmployeeIds_shouldNotFilterByLicense_whenRequiresLicenseIsFalse() {
        UUID unlicensedId = UUID.randomUUID();
        ClinicService service = service(List.of(EmployeeRole.VETERINARIAN), false);

        List<UUID> result = ClinicServiceEligibility.resolveEmployeeIds(service, List.of(
                new EmployeeLicense(unlicensedId, EmployeeRole.VETERINARIAN, false)));

        assertThat(result).containsExactly(unlicensedId);
    }

    @Test
    void resolveEmployeeIds_shouldIncludeTheSoleOwner_withDefaultAllowedRoles() {
        UUID ownerId = UUID.randomUUID();
        ClinicService service = service(ClinicServiceValidation.DEFAULT_ALLOWED_ROLES, false);

        List<UUID> result = ClinicServiceEligibility.resolveEmployeeIds(service, List.of(
                new EmployeeLicense(ownerId, EmployeeRole.OWNER, false)));

        assertThat(result).containsExactly(ownerId);
    }

    @Test
    void resolveEmployeeIds_shouldReturnEmpty_whenNoEmployeeMatches() {
        ClinicService service = service(List.of(EmployeeRole.VETERINARIAN), false);

        List<UUID> result = ClinicServiceEligibility.resolveEmployeeIds(service, List.of(
                new EmployeeLicense(UUID.randomUUID(), EmployeeRole.RECEPTIONIST, false)));

        assertThat(result).isEmpty();
    }
}

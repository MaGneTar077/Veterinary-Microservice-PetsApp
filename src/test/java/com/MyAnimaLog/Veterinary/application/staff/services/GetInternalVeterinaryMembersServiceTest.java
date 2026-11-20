package com.MyAnimaLog.Veterinary.application.staff.services;

import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.UserServicePort;
import com.MyAnimaLog.Veterinary.application.staff.dto.InternalVeterinaryMemberResponse;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort.EmployeeWithLicense;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.security.Permission;
import com.MyAnimaLog.Veterinary.domain.security.RolePermissions;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.ExternalServiceUnavailableException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VeterinaryNotFoundException;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetInternalVeterinaryMembersServiceTest {

    @Mock
    private VeterinaryRepositoryPort veterinaryRepositoryPort;
    @Mock
    private VeterinaryEmployeeRepositoryPort employeeRepositoryPort;
    @Mock
    private UserServicePort userServicePort;

    @InjectMocks
    private GetInternalVeterinaryMembersService getInternalVeterinaryMembersService;

    private UUID veterinaryId;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();
    }

    private void clinicExists(VeterinaryStatus status) {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(Veterinary.builder()
                .id(veterinaryId).name("Clínica").city("Bogotá").tenantId(UUID.randomUUID().toString())
                .status(status).active(status == VeterinaryStatus.ACTIVE).build()));
    }

    @Test
    void list_shouldOnlyReturnActiveMembers_byDefault() {
        clinicExists(VeterinaryStatus.ACTIVE);
        UUID activeId = UUID.randomUUID();
        UUID userActive = UUID.randomUUID();
        UUID userRemoved = UUID.randomUUID();
        when(employeeRepositoryPort.findAllWithLicense(veterinaryId)).thenReturn(List.of(
                new EmployeeWithLicense(activeId, userActive, EmployeeRole.VETERINARIAN, true, true),
                new EmployeeWithLicense(UUID.randomUUID(), userRemoved, EmployeeRole.ASSISTANT, false, false)));
        when(userServicePort.getUser(userActive))
                .thenReturn(new UserServicePort.UserInfo(userActive, "a@x.com", true, "Ana"));

        List<InternalVeterinaryMemberResponse> result = getInternalVeterinaryMembersService.list(veterinaryId, false);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmployeeId()).isEqualTo(activeId);
    }

    @Test
    void list_shouldIncludeRemovedEmployees_withActiveFalse_whenIncludeInactive() {
        clinicExists(VeterinaryStatus.ACTIVE);
        UUID userActive = UUID.randomUUID();
        UUID userRemoved = UUID.randomUUID();
        when(employeeRepositoryPort.findAllWithLicense(veterinaryId)).thenReturn(List.of(
                new EmployeeWithLicense(UUID.randomUUID(), userActive, EmployeeRole.VETERINARIAN, true, true),
                new EmployeeWithLicense(UUID.randomUUID(), userRemoved, EmployeeRole.ASSISTANT, false, false)));
        when(userServicePort.getUser(any())).thenReturn(null);

        List<InternalVeterinaryMemberResponse> result = getInternalVeterinaryMembersService.list(veterinaryId, true);

        assertThat(result).hasSize(2);
        assertThat(result).filteredOn(m -> !m.isActive()).extracting(InternalVeterinaryMemberResponse::isActive)
                .containsExactly(false);
    }

    @Test
    void list_shouldGiveNoPermissions_toARemovedEmployee_regardlessOfRole() {
        clinicExists(VeterinaryStatus.ACTIVE);
        UUID userId = UUID.randomUUID();
        when(employeeRepositoryPort.findAllWithLicense(veterinaryId)).thenReturn(List.of(
                new EmployeeWithLicense(UUID.randomUUID(), userId, EmployeeRole.OWNER, false, true)));
        when(userServicePort.getUser(userId)).thenReturn(null);

        List<InternalVeterinaryMemberResponse> result = getInternalVeterinaryMembersService.list(veterinaryId, true);

        assertThat(result.get(0).getPermissions()).isEmpty();
    }

    @Test
    void list_shouldMatchRolePermissionsResolve_forAnActiveMember() {
        clinicExists(VeterinaryStatus.ACTIVE);
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(employeeRepositoryPort.findAllWithLicense(veterinaryId)).thenReturn(List.of(
                new EmployeeWithLicense(employeeId, userId, EmployeeRole.VETERINARIAN, true, true)));
        when(userServicePort.getUser(userId)).thenReturn(null);

        List<InternalVeterinaryMemberResponse> result = getInternalVeterinaryMembersService.list(veterinaryId, false);

        Set<Permission> expected = RolePermissions.resolve(EmployeeRole.VETERINARIAN, true, VeterinaryStatus.ACTIVE);
        assertThat(result.get(0).getPermissions())
                .containsExactlyInAnyOrderElementsOf(expected.stream().map(Permission::name).toList());
    }

    @Test
    void list_shouldReadLicenseFromTheEmployeeWithLicenseQuery_notTheClaim() {
        clinicExists(VeterinaryStatus.ACTIVE);
        UUID userId = UUID.randomUUID();
        when(employeeRepositoryPort.findAllWithLicense(veterinaryId)).thenReturn(List.of(
                new EmployeeWithLicense(UUID.randomUUID(), userId, EmployeeRole.VETERINARIAN, true, true)));
        when(userServicePort.getUser(userId)).thenReturn(null);

        assertThat(getInternalVeterinaryMembersService.list(veterinaryId, false).get(0).isLicensed()).isTrue();
    }

    @Test
    void list_shouldReturnNullName_whenTheUserServiceFails() {
        clinicExists(VeterinaryStatus.ACTIVE);
        UUID userId = UUID.randomUUID();
        when(employeeRepositoryPort.findAllWithLicense(veterinaryId)).thenReturn(List.of(
                new EmployeeWithLicense(UUID.randomUUID(), userId, EmployeeRole.VETERINARIAN, true, true)));
        when(userServicePort.getUser(userId)).thenThrow(new ExternalServiceUnavailableException("down"));

        List<InternalVeterinaryMemberResponse> result = getInternalVeterinaryMembersService.list(veterinaryId, false);

        assertThat(result.get(0).getName()).isNull();
    }

    @Test
    void list_shouldNotExposeEmailOrPhone() {
        assertThat(InternalVeterinaryMemberResponse.class.getDeclaredFields())
                .extracting(java.lang.reflect.Field::getName)
                .containsExactlyInAnyOrder("employeeId", "userId", "name", "role", "active", "licensed",
                        "permissions");
    }

    @Test
    void list_shouldThrowVeterinaryNotFound_whenTheClinicDoesNotExist() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> getInternalVeterinaryMembersService.list(veterinaryId, false))
                .isInstanceOf(VeterinaryNotFoundException.class);
    }
}

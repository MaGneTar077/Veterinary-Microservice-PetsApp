package com.MyAnimaLog.Veterinary.application.shared.services;

import com.MyAnimaLog.Veterinary.application.shared.dto.AuthenticatedUser;
import com.MyAnimaLog.Veterinary.application.shared.dto.ClinicContext;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.AuthenticatedUserPort;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.security.Permission;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.NotVeterinaryOwnerException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.TenantMismatchException;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.domain.staff.model.VeterinaryEmployee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VeterinaryAuthorizationServiceTest {

    @Mock
    private AuthenticatedUserPort authenticatedUserPort;

    @Mock
    private VeterinaryEmployeeRepositoryPort employeeRepositoryPort;

    @InjectMocks
    private VeterinaryAuthorizationService authorizationService;

    private UUID veterinaryId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    private AuthenticatedUser userWithoutClinicToken() {
        return new AuthenticatedUser(userId, "owner@example.com", true, false, Optional.empty());
    }

    private AuthenticatedUser userWithClinicToken(UUID clinicVetId, EmployeeRole role, VeterinaryStatus status) {
        ClinicContext context = new ClinicContext(clinicVetId, UUID.randomUUID(), role, false, status);
        return new AuthenticatedUser(userId, "owner@example.com", true, false, Optional.of(context));
    }

    private VeterinaryEmployee employeeRow(EmployeeRole role, boolean active) {
        return VeterinaryEmployee.builder()
                .id(UUID.randomUUID())
                .veterinaryId(veterinaryId)
                .userId(userId)
                .role(role)
                .active(active)
                .build();
    }

    @Test
    void requireVeterinaryOwner_shouldPass_whenCallerIsActiveOwner_withNormalToken() {
        when(authenticatedUserPort.current()).thenReturn(userWithoutClinicToken());
        when(employeeRepositoryPort.findByVeterinaryIdAndUserId(veterinaryId, userId))
                .thenReturn(Optional.of(employeeRow(EmployeeRole.OWNER, true)));

        assertThatNoException().isThrownBy(() -> authorizationService.requireVeterinaryOwner(veterinaryId));
    }

    @Test
    void requireVeterinaryOwner_shouldPass_evenWhenClinicIsRejected_asLongAsCallerIsActiveOwner() {
        when(authenticatedUserPort.current())
                .thenReturn(userWithClinicToken(veterinaryId, EmployeeRole.OWNER, VeterinaryStatus.REJECTED));
        when(employeeRepositoryPort.findByVeterinaryIdAndUserId(veterinaryId, userId))
                .thenReturn(Optional.of(employeeRow(EmployeeRole.OWNER, true)));

        assertThatNoException().isThrownBy(() -> authorizationService.requireVeterinaryOwner(veterinaryId));
    }

    @Test
    void requireVeterinaryOwner_shouldThrowNotVeterinaryOwner_whenCallerIsAdminOfSameClinic() {
        when(authenticatedUserPort.current()).thenReturn(userWithoutClinicToken());
        when(employeeRepositoryPort.findByVeterinaryIdAndUserId(veterinaryId, userId))
                .thenReturn(Optional.of(employeeRow(EmployeeRole.ADMIN, true)));

        assertThatThrownBy(() -> authorizationService.requireVeterinaryOwner(veterinaryId))
                .isInstanceOf(NotVeterinaryOwnerException.class);
    }

    @Test
    void requireVeterinaryOwner_shouldThrowNotVeterinaryOwner_whenCallerHasNoEmployeeRow() {
        when(authenticatedUserPort.current()).thenReturn(userWithoutClinicToken());
        when(employeeRepositoryPort.findByVeterinaryIdAndUserId(veterinaryId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authorizationService.requireVeterinaryOwner(veterinaryId))
                .isInstanceOf(NotVeterinaryOwnerException.class);
    }

    @Test
    void requireVeterinaryOwner_shouldThrowNotVeterinaryOwner_whenOwnerRowIsInactive() {
        when(authenticatedUserPort.current()).thenReturn(userWithoutClinicToken());
        when(employeeRepositoryPort.findByVeterinaryIdAndUserId(veterinaryId, userId))
                .thenReturn(Optional.of(employeeRow(EmployeeRole.OWNER, false)));

        assertThatThrownBy(() -> authorizationService.requireVeterinaryOwner(veterinaryId))
                .isInstanceOf(NotVeterinaryOwnerException.class);
    }

    @Test
    void requireVeterinaryOwner_shouldThrowTenantMismatch_whenClinicTokenIsForAnotherVeterinary() {
        UUID otherVetId = UUID.randomUUID();
        when(authenticatedUserPort.current())
                .thenReturn(userWithClinicToken(otherVetId, EmployeeRole.OWNER, VeterinaryStatus.ACTIVE));

        assertThatThrownBy(() -> authorizationService.requireVeterinaryOwner(veterinaryId))
                .isInstanceOf(TenantMismatchException.class);

        verify(employeeRepositoryPort, never()).findByVeterinaryIdAndUserId(veterinaryId, userId);
    }

    @Test
    void requireOwnerOrPermission_shouldPass_whenClinicTokenHasThePermission_withoutHittingTheDatabase() {
        when(authenticatedUserPort.current())
                .thenReturn(userWithClinicToken(veterinaryId, EmployeeRole.ADMIN, VeterinaryStatus.ACTIVE));

        assertThatNoException().isThrownBy(() ->
                authorizationService.requireOwnerOrPermission(veterinaryId, Permission.CLINIC_CONFIGURE));

        verify(employeeRepositoryPort, never()).findByVeterinaryIdAndUserId(veterinaryId, userId);
    }

    @Test
    void requireOwnerOrPermission_shouldFallBackToOwnerCheck_whenClinicTokenLacksThePermission() {
        when(authenticatedUserPort.current())
                .thenReturn(userWithClinicToken(veterinaryId, EmployeeRole.VETERINARIAN, VeterinaryStatus.ACTIVE));
        when(employeeRepositoryPort.findByVeterinaryIdAndUserId(veterinaryId, userId))
                .thenReturn(Optional.of(employeeRow(EmployeeRole.OWNER, true)));

        assertThatNoException().isThrownBy(() ->
                authorizationService.requireOwnerOrPermission(veterinaryId, Permission.CLINIC_CONFIGURE));
    }

    @Test
    void requireOwnerOrPermission_shouldFallBackToOwnerCheck_whenCallerHasNormalToken() {
        when(authenticatedUserPort.current()).thenReturn(userWithoutClinicToken());
        when(employeeRepositoryPort.findByVeterinaryIdAndUserId(veterinaryId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authorizationService.requireOwnerOrPermission(veterinaryId, Permission.CLINIC_CONFIGURE))
                .isInstanceOf(NotVeterinaryOwnerException.class);
    }
}

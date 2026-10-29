package com.MyAnimaLog.Veterinary.application.staff.services;

import com.MyAnimaLog.Veterinary.application.shared.dto.ClinicContext;
import com.MyAnimaLog.Veterinary.application.shared.dto.DomainEvent;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.EventPublisherPort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.application.staff.dto.UpdateEmployeeRoleRequest;
import com.MyAnimaLog.Veterinary.application.staff.dto.UpdateEmployeeRoleResponse;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.security.Permission;
import com.MyAnimaLog.Veterinary.domain.shared.enums.VeterinaryEventType;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.ClinicSuspendedException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.InsufficientPermissionException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.NotClinicMemberException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.TenantMismatchException;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.CannotModifyOwnerException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.CannotModifyPeerAdminException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.CannotModifySelfException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.EmployeeNotFoundException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.InvalidEmployeeRoleException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.OnlyOwnerCanManageAdminsException;
import com.MyAnimaLog.Veterinary.domain.staff.model.VeterinaryEmployee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UpdateEmployeeRoleServiceTest {

    @Mock
    private VeterinaryEmployeeRepositoryPort employeeRepositoryPort;

    @Mock
    private VeterinaryAuthorizationService authorizationService;

    @Mock
    private EventPublisherPort eventPublisherPort;

    @InjectMocks
    private UpdateEmployeeRoleService updateEmployeeRoleService;

    private UUID employeeId;
    private UUID veterinaryId;
    private UUID actingEmployeeId;
    private VeterinaryEmployee target;

    @BeforeEach
    void setUp() {
        employeeId = UUID.randomUUID();
        veterinaryId = UUID.randomUUID();
        actingEmployeeId = UUID.randomUUID();

        target = VeterinaryEmployee.builder()
                .id(employeeId)
                .veterinaryId(veterinaryId)
                .userId(UUID.randomUUID())
                .role(EmployeeRole.VETERINARIAN)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(employeeRepositoryPort.findById(employeeId)).thenReturn(Optional.of(target));
        when(employeeRepositoryPort.save(any(VeterinaryEmployee.class))).thenAnswer(c -> c.getArgument(0));
        actingAs(EmployeeRole.OWNER, VeterinaryStatus.ACTIVE);
    }

    /** El rol del actor llega en el ClinicContext, que el servicio de autorización saca de la base. */
    private void actingAs(EmployeeRole role, VeterinaryStatus clinicStatus) {
        when(authorizationService.require(veterinaryId, Permission.STAFF_MANAGE)).thenReturn(
                new ClinicContext(veterinaryId, actingEmployeeId, role, false, clinicStatus));
    }

    private UpdateEmployeeRoleResponse updateTo(EmployeeRole role) {
        return updateEmployeeRoleService.updateRole(veterinaryId, employeeId,
                UpdateEmployeeRoleRequest.builder().role(role).build());
    }

    @Test
    void updateRole_shouldChangeTheRole() {
        UpdateEmployeeRoleResponse response = updateTo(EmployeeRole.RECEPTIONIST);

        assertThat(response.getId()).isEqualTo(employeeId);
        assertThat(response.getRole()).isEqualTo(EmployeeRole.RECEPTIONIST);
        verify(employeeRepositoryPort).save(any(VeterinaryEmployee.class));
    }

    @Test
    void updateRole_shouldRequireStaffManageOverTheEmployeeClinic() {
        updateTo(EmployeeRole.RECEPTIONIST);

        verify(authorizationService).require(veterinaryId, Permission.STAFF_MANAGE);
    }

    @Test
    void updateRole_shouldWorkWithoutAVeterinaryIdInThePath_forTheDeprecatedRoute() {
        // La ruta vieja /api/veterinary/employees/{id}/role no lleva la clínica: se toma del empleado.
        UpdateEmployeeRoleResponse response = updateEmployeeRoleService.updateRole(null, employeeId,
                UpdateEmployeeRoleRequest.builder().role(EmployeeRole.ASSISTANT).build());

        assertThat(response.getRole()).isEqualTo(EmployeeRole.ASSISTANT);
        verify(authorizationService).require(veterinaryId, Permission.STAFF_MANAGE);
    }

    @Test
    void updateRole_shouldThrowEmployeeNotFoundException_whenTheEmployeeBelongsToAnotherClinic() {
        UUID anotherClinic = UUID.randomUUID();

        assertThatThrownBy(() -> updateEmployeeRoleService.updateRole(anotherClinic, employeeId,
                UpdateEmployeeRoleRequest.builder().role(EmployeeRole.ASSISTANT).build()))
                .isInstanceOf(EmployeeNotFoundException.class);

        verify(employeeRepositoryPort, never()).save(any());
    }

    @Test
    void updateRole_shouldThrowInvalidEmployeeRoleException_whenRoleIsNullOrOwner() {
        assertThatThrownBy(() -> updateTo(null)).isInstanceOf(InvalidEmployeeRoleException.class);
        assertThatThrownBy(() -> updateTo(EmployeeRole.OWNER)).isInstanceOf(InvalidEmployeeRoleException.class);

        verify(employeeRepositoryPort, never()).save(any());
    }

    @Test
    void updateRole_shouldThrowEmployeeNotFoundException_whenEmployeeDoesNotExist() {
        when(employeeRepositoryPort.findById(employeeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> updateTo(EmployeeRole.ASSISTANT))
                .isInstanceOf(EmployeeNotFoundException.class);
    }

    @Test
    void updateRole_shouldThrowCannotModifySelfException_whenActorTargetsItsOwnRecord() {
        when(employeeRepositoryPort.findById(employeeId))
                .thenReturn(Optional.of(target.toBuilder().id(actingEmployeeId).build()));

        assertThatThrownBy(() -> updateEmployeeRoleService.updateRole(veterinaryId, employeeId,
                UpdateEmployeeRoleRequest.builder().role(EmployeeRole.ASSISTANT).build()))
                .isInstanceOf(CannotModifySelfException.class);

        verify(employeeRepositoryPort, never()).save(any());
    }

    @Test
    void updateRole_shouldThrowCannotModifyOwnerException_whenTargetIsTheOwner() {
        when(employeeRepositoryPort.findById(employeeId))
                .thenReturn(Optional.of(target.toBuilder().role(EmployeeRole.OWNER).build()));

        assertThatThrownBy(() -> updateTo(EmployeeRole.ASSISTANT))
                .isInstanceOf(CannotModifyOwnerException.class);
    }

    @Test
    void updateRole_shouldThrowCannotModifyPeerAdminException_whenAnAdminTargetsAnotherAdmin() {
        actingAs(EmployeeRole.ADMIN, VeterinaryStatus.ACTIVE);
        when(employeeRepositoryPort.findById(employeeId))
                .thenReturn(Optional.of(target.toBuilder().role(EmployeeRole.ADMIN).build()));

        assertThatThrownBy(() -> updateTo(EmployeeRole.ASSISTANT))
                .isInstanceOf(CannotModifyPeerAdminException.class);
    }

    @Test
    void updateRole_shouldThrowOnlyOwnerCanManageAdminsException_whenAnAdminPromotesSomeoneToAdmin() {
        actingAs(EmployeeRole.ADMIN, VeterinaryStatus.ACTIVE);

        assertThatThrownBy(() -> updateTo(EmployeeRole.ADMIN))
                .isInstanceOf(OnlyOwnerCanManageAdminsException.class);

        verify(employeeRepositoryPort, never()).save(any());
    }

    @Test
    void updateRole_shouldLetTheOwnerPromoteSomeoneToAdmin() {
        assertThat(updateTo(EmployeeRole.ADMIN).getRole()).isEqualTo(EmployeeRole.ADMIN);
    }

    @Test
    void updateRole_shouldPublishEmployeeRoleUpdatedToTheEmployeeWithBothRoles() {
        updateTo(EmployeeRole.RECEPTIONIST);

        ArgumentCaptor<DomainEvent> captor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisherPort).publish(captor.capture());
        DomainEvent event = captor.getValue();
        assertThat(event.eventType()).isEqualTo(VeterinaryEventType.EMPLOYEE_ROLE_UPDATED);
        assertThat(event.userId()).isEqualTo(target.getUserId());
        assertThat(event.payload()).containsEntry("oldRole", "VETERINARIAN");
        assertThat(event.payload()).containsEntry("newRole", "RECEPTIONIST");
        assertThat(event.metadata().veterinaryId()).isEqualTo(veterinaryId);
    }

    @Test
    void updateRole_shouldNotTouchAnythingWhenAuthorizationFails() {
        for (RuntimeException rejection : new RuntimeException[]{
                new InsufficientPermissionException(Permission.STAFF_MANAGE),
                new TenantMismatchException(),
                new NotClinicMemberException(),
                new ClinicSuspendedException()}) {
            // doThrow y no when(...).thenThrow(...): con when() la llamada dentro del propio when
            // ejecutaría el stub anterior y lanzaría antes de re-stubear.
            doThrow(rejection).when(authorizationService).require(veterinaryId, Permission.STAFF_MANAGE);

            assertThatThrownBy(() -> updateTo(EmployeeRole.ASSISTANT)).isInstanceOf(rejection.getClass());
        }

        verify(employeeRepositoryPort, never()).save(any());
        verify(eventPublisherPort, never()).publish(any());
    }
}

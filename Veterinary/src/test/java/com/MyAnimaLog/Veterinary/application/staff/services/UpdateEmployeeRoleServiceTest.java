package com.MyAnimaLog.Veterinary.application.staff.services;

import com.MyAnimaLog.Veterinary.application.shared.dto.ClinicContext;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.application.staff.dto.UpdateEmployeeRoleRequest;
import com.MyAnimaLog.Veterinary.application.staff.dto.UpdateEmployeeRoleResponse;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.security.Permission;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.CannotModifyOwnerException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.CannotModifyPeerAdminException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.CannotModifySelfException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.EmployeeNotFoundException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.InvalidEmployeeRoleException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.InsufficientPermissionException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.TenantMismatchException;
import com.MyAnimaLog.Veterinary.domain.staff.model.VeterinaryEmployee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateEmployeeRoleServiceTest {

    @Mock
    private VeterinaryEmployeeRepositoryPort employeeRepositoryPort;

    @Mock
    private VeterinaryAuthorizationService authorizationService;

    @InjectMocks
    private UpdateEmployeeRoleService updateEmployeeRoleService;

    private UUID employeeId;
    private UUID veterinaryId;
    private UUID actingEmployeeId;
    private VeterinaryEmployee existingEmployee;
    private VeterinaryEmployee updatedEmployee;
    private VeterinaryEmployee actingEmployee;
    private ClinicContext callerContext;

    @BeforeEach
    void setUp() {
        employeeId = UUID.randomUUID();
        veterinaryId = UUID.randomUUID();
        actingEmployeeId = UUID.randomUUID();

        existingEmployee = VeterinaryEmployee.builder()
                .id(employeeId)
                .veterinaryId(veterinaryId)
                .userId(UUID.randomUUID())
                .role(EmployeeRole.VETERINARIAN)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        updatedEmployee = existingEmployee.toBuilder()
                .role(EmployeeRole.ADMIN)
                .build();

        actingEmployee = VeterinaryEmployee.builder()
                .id(actingEmployeeId)
                .veterinaryId(veterinaryId)
                .userId(UUID.randomUUID())
                .role(EmployeeRole.ADMIN)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        callerContext = new ClinicContext(veterinaryId, actingEmployeeId, EmployeeRole.ADMIN, false, VeterinaryStatus.ACTIVE);

        lenient().when(authorizationService.require(veterinaryId, Permission.STAFF_MANAGE)).thenReturn(callerContext);
        lenient().when(employeeRepositoryPort.findById(actingEmployeeId)).thenReturn(Optional.of(actingEmployee));
    }

    @Test
    void updateRole_shouldReturnResponse_whenRequestIsValid() {
        when(employeeRepositoryPort.findById(employeeId)).thenReturn(Optional.of(existingEmployee));
        when(employeeRepositoryPort.save(any(VeterinaryEmployee.class))).thenReturn(updatedEmployee);

        UpdateEmployeeRoleResponse response = updateEmployeeRoleService.updateRole(
                employeeId,
                UpdateEmployeeRoleRequest.builder().role(EmployeeRole.ADMIN).build()
        );

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(employeeId);
        assertThat(response.getRole()).isEqualTo(EmployeeRole.ADMIN);
    }

    @Test
    void updateRole_shouldThrowInvalidEmployeeRoleException_whenRoleIsNull() {
        assertThatThrownBy(() ->
                updateEmployeeRoleService.updateRole(
                        employeeId,
                        UpdateEmployeeRoleRequest.builder().role(null).build()
                )
        ).isInstanceOf(InvalidEmployeeRoleException.class);
    }

    @Test
    void updateRole_shouldThrowEmployeeNotFoundException_whenEmployeeDoesNotExist() {
        when(employeeRepositoryPort.findById(employeeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                updateEmployeeRoleService.updateRole(
                        employeeId,
                        UpdateEmployeeRoleRequest.builder().role(EmployeeRole.ADMIN).build()
                )
        ).isInstanceOf(EmployeeNotFoundException.class);
    }

    @Test
    void updateRole_shouldCallSave_withNewRole() {
        when(employeeRepositoryPort.findById(employeeId)).thenReturn(Optional.of(existingEmployee));
        when(employeeRepositoryPort.save(any(VeterinaryEmployee.class))).thenReturn(updatedEmployee);

        updateEmployeeRoleService.updateRole(
                employeeId,
                UpdateEmployeeRoleRequest.builder().role(EmployeeRole.ADMIN).build()
        );

        verify(employeeRepositoryPort, times(1)).save(argThat(e ->
                e.getRole().equals(EmployeeRole.ADMIN)
        ));
    }

    @Test
    void updateRole_shouldCallSave_once() {
        when(employeeRepositoryPort.findById(employeeId)).thenReturn(Optional.of(existingEmployee));
        when(employeeRepositoryPort.save(any(VeterinaryEmployee.class))).thenReturn(updatedEmployee);

        updateEmployeeRoleService.updateRole(
                employeeId,
                UpdateEmployeeRoleRequest.builder().role(EmployeeRole.ADMIN).build()
        );

        verify(employeeRepositoryPort, times(1)).save(any(VeterinaryEmployee.class));
    }

    @Test
    void updateRole_shouldNeverCallSave_whenRoleIsNull() {
        assertThatThrownBy(() ->
                updateEmployeeRoleService.updateRole(
                        employeeId,
                        UpdateEmployeeRoleRequest.builder().role(null).build()
                )
        ).isInstanceOf(InvalidEmployeeRoleException.class);

        verify(employeeRepositoryPort, never()).save(any());
    }

    @Test
    void updateRole_shouldNeverCallSave_whenEmployeeNotFound() {
        when(employeeRepositoryPort.findById(employeeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                updateEmployeeRoleService.updateRole(
                        employeeId,
                        UpdateEmployeeRoleRequest.builder().role(EmployeeRole.ADMIN).build()
                )
        ).isInstanceOf(EmployeeNotFoundException.class);

        verify(employeeRepositoryPort, never()).save(any());
    }

    @Test
    void updateRole_shouldThrowCannotModifySelfException_whenActorTargetsOwnEmployeeRecord() {
        VeterinaryEmployee selfEmployee = existingEmployee.toBuilder().id(actingEmployeeId).build();
        when(employeeRepositoryPort.findById(actingEmployeeId)).thenReturn(Optional.of(selfEmployee));

        assertThatThrownBy(() ->
                updateEmployeeRoleService.updateRole(
                        actingEmployeeId,
                        UpdateEmployeeRoleRequest.builder().role(EmployeeRole.ASSISTANT).build()
                )
        ).isInstanceOf(CannotModifySelfException.class);

        verify(employeeRepositoryPort, never()).save(any());
    }

    @Test
    void updateRole_shouldThrowInvalidEmployeeRoleException_whenAssigningOwnerRole() {
        assertThatThrownBy(() ->
                updateEmployeeRoleService.updateRole(
                        employeeId,
                        UpdateEmployeeRoleRequest.builder().role(EmployeeRole.OWNER).build()
                )
        ).isInstanceOf(InvalidEmployeeRoleException.class);

        verify(employeeRepositoryPort, never()).save(any());
    }

    @Test
    void updateRole_shouldThrowCannotModifyOwnerException_whenTargetIsOwner() {
        VeterinaryEmployee owner = existingEmployee.toBuilder().role(EmployeeRole.OWNER).build();
        when(employeeRepositoryPort.findById(employeeId)).thenReturn(Optional.of(owner));

        assertThatThrownBy(() ->
                updateEmployeeRoleService.updateRole(
                        employeeId,
                        UpdateEmployeeRoleRequest.builder().role(EmployeeRole.VETERINARIAN).build()
                )
        ).isInstanceOf(CannotModifyOwnerException.class);

        verify(employeeRepositoryPort, never()).save(any());
    }

    @Test
    void updateRole_shouldThrowCannotModifyPeerAdminException_whenActingAdminTargetsAnotherAdmin() {
        VeterinaryEmployee targetAdmin = existingEmployee.toBuilder().role(EmployeeRole.ADMIN).build();
        when(employeeRepositoryPort.findById(employeeId)).thenReturn(Optional.of(targetAdmin));

        assertThatThrownBy(() ->
                updateEmployeeRoleService.updateRole(
                        employeeId,
                        UpdateEmployeeRoleRequest.builder().role(EmployeeRole.VETERINARIAN).build()
                )
        ).isInstanceOf(CannotModifyPeerAdminException.class);

        verify(employeeRepositoryPort, never()).save(any());
    }

    @Test
    void updateRole_shouldThrowInsufficientPermissionException_whenCallerLacksStaffManage() {
        when(employeeRepositoryPort.findById(employeeId)).thenReturn(Optional.of(existingEmployee));
        when(authorizationService.require(veterinaryId, Permission.STAFF_MANAGE))
                .thenThrow(new InsufficientPermissionException(Permission.STAFF_MANAGE));

        assertThatThrownBy(() ->
                updateEmployeeRoleService.updateRole(
                        employeeId,
                        UpdateEmployeeRoleRequest.builder().role(EmployeeRole.ADMIN).build()
                )
        ).isInstanceOf(InsufficientPermissionException.class);

        verify(employeeRepositoryPort, never()).save(any());
    }

    @Test
    void updateRole_shouldThrowTenantMismatchException_whenCallerBelongsToAnotherVeterinary() {
        when(employeeRepositoryPort.findById(employeeId)).thenReturn(Optional.of(existingEmployee));
        when(authorizationService.require(veterinaryId, Permission.STAFF_MANAGE))
                .thenThrow(new TenantMismatchException());

        assertThatThrownBy(() ->
                updateEmployeeRoleService.updateRole(
                        employeeId,
                        UpdateEmployeeRoleRequest.builder().role(EmployeeRole.ADMIN).build()
                )
        ).isInstanceOf(TenantMismatchException.class);

        verify(employeeRepositoryPort, never()).save(any());
    }
}
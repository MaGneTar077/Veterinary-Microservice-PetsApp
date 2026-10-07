package com.MyAnimaLog.Veterinary.application.staff.services;

import com.MyAnimaLog.Veterinary.application.shared.dto.ClinicContext;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.application.staff.dto.DeActivateEmployeeResponse;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.security.Permission;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.InsufficientPermissionException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.TenantMismatchException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.CannotModifySelfException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.EmployeeNotFoundException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.LastAdminException;
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
class DeActivateEmployeeServiceTest {

    @Mock
    private VeterinaryEmployeeRepositoryPort employeeRepositoryPort;

    @Mock
    private VeterinaryAuthorizationService authorizationService;

    @InjectMocks
    private DeActivateEmployeeService deActivateEmployeeService;

    private UUID employeeId;
    private UUID veterinaryId;
    private UUID actingEmployeeId;
    private VeterinaryEmployee activeEmployee;
    private VeterinaryEmployee deactivatedEmployee;
    private VeterinaryEmployee actingEmployee;
    private ClinicContext callerContext;

    @BeforeEach
    void setUp() {
        employeeId = UUID.randomUUID();
        veterinaryId = UUID.randomUUID();
        actingEmployeeId = UUID.randomUUID();

        activeEmployee = VeterinaryEmployee.builder()
                .id(employeeId)
                .veterinaryId(veterinaryId)
                .userId(UUID.randomUUID())
                .role(EmployeeRole.ADMIN)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        deactivatedEmployee = activeEmployee.toBuilder()
                .active(false)
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
        lenient().when(employeeRepositoryPort.countByVeterinaryIdAndRoleAndActiveTrue(veterinaryId, EmployeeRole.ADMIN))
                .thenReturn(2L);
    }

    @Test
    void deActivate_shouldReturnResponse_withActiveFalse() {
        when(employeeRepositoryPort.findById(employeeId)).thenReturn(Optional.of(activeEmployee));
        when(employeeRepositoryPort.save(any(VeterinaryEmployee.class))).thenReturn(deactivatedEmployee);

        DeActivateEmployeeResponse response = deActivateEmployeeService.deActivate(employeeId);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(employeeId);
        assertThat(response.getActive()).isFalse();
        assertThat(response.getRole()).isEqualTo(EmployeeRole.ADMIN);
    }

    @Test
    void deActivate_shouldThrowEmployeeNotFoundException_whenEmployeeDoesNotExist() {
        when(employeeRepositoryPort.findById(employeeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                deActivateEmployeeService.deActivate(employeeId)
        ).isInstanceOf(EmployeeNotFoundException.class);
    }

    @Test
    void deActivate_shouldCallSave_withActiveFalse() {
        when(employeeRepositoryPort.findById(employeeId)).thenReturn(Optional.of(activeEmployee));
        when(employeeRepositoryPort.save(any(VeterinaryEmployee.class))).thenReturn(deactivatedEmployee);

        deActivateEmployeeService.deActivate(employeeId);

        verify(employeeRepositoryPort, times(1)).save(argThat(e -> !e.getActive()));
    }

    @Test
    void deActivate_shouldCallSave_once() {
        when(employeeRepositoryPort.findById(employeeId)).thenReturn(Optional.of(activeEmployee));
        when(employeeRepositoryPort.save(any(VeterinaryEmployee.class))).thenReturn(deactivatedEmployee);

        deActivateEmployeeService.deActivate(employeeId);

        verify(employeeRepositoryPort, times(1)).save(any(VeterinaryEmployee.class));
    }

    @Test
    void deActivate_shouldNeverCallSave_whenEmployeeNotFound() {
        when(employeeRepositoryPort.findById(employeeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                deActivateEmployeeService.deActivate(employeeId)
        ).isInstanceOf(EmployeeNotFoundException.class);

        verify(employeeRepositoryPort, never()).save(any());
    }

    @Test
    void deActivate_shouldThrowCannotModifySelfException_whenActorTargetsOwnEmployeeRecord() {
        VeterinaryEmployee selfEmployee = activeEmployee.toBuilder().id(actingEmployeeId).build();
        when(employeeRepositoryPort.findById(actingEmployeeId)).thenReturn(Optional.of(selfEmployee));

        assertThatThrownBy(() ->
                deActivateEmployeeService.deActivate(actingEmployeeId)
        ).isInstanceOf(CannotModifySelfException.class);

        verify(employeeRepositoryPort, never()).save(any());
    }

    @Test
    void deActivate_shouldThrowLastAdminException_whenDeactivatingTheOnlyActiveAdmin() {
        when(employeeRepositoryPort.findById(employeeId)).thenReturn(Optional.of(activeEmployee));
        when(employeeRepositoryPort.countByVeterinaryIdAndRoleAndActiveTrue(veterinaryId, EmployeeRole.ADMIN))
                .thenReturn(1L);

        assertThatThrownBy(() ->
                deActivateEmployeeService.deActivate(employeeId)
        ).isInstanceOf(LastAdminException.class);

        verify(employeeRepositoryPort, never()).save(any());
    }

    @Test
    void deActivate_shouldThrowInsufficientPermissionException_whenCallerLacksStaffManage() {
        when(employeeRepositoryPort.findById(employeeId)).thenReturn(Optional.of(activeEmployee));
        when(authorizationService.require(veterinaryId, Permission.STAFF_MANAGE))
                .thenThrow(new InsufficientPermissionException(Permission.STAFF_MANAGE));

        assertThatThrownBy(() ->
                deActivateEmployeeService.deActivate(employeeId)
        ).isInstanceOf(InsufficientPermissionException.class);

        verify(employeeRepositoryPort, never()).save(any());
    }

    @Test
    void deActivate_shouldThrowTenantMismatchException_whenCallerBelongsToAnotherVeterinary() {
        when(employeeRepositoryPort.findById(employeeId)).thenReturn(Optional.of(activeEmployee));
        when(authorizationService.require(veterinaryId, Permission.STAFF_MANAGE))
                .thenThrow(new TenantMismatchException());

        assertThatThrownBy(() ->
                deActivateEmployeeService.deActivate(employeeId)
        ).isInstanceOf(TenantMismatchException.class);

        verify(employeeRepositoryPort, never()).save(any());
    }
}
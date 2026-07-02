package com.MyAnimaLog.Veterinary.application.services;

import com.MyAnimaLog.Veterinary.application.dto.UpdateEmployeeRoleRequest;
import com.MyAnimaLog.Veterinary.application.dto.UpdateEmployeeRoleResponse;
import com.MyAnimaLog.Veterinary.application.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.domain.exceptions.EmployeeNotFoundException;
import com.MyAnimaLog.Veterinary.domain.exceptions.InvalidEmployeeRoleException;
import com.MyAnimaLog.Veterinary.domain.model.VeterinaryEmployee;
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

    @InjectMocks
    private UpdateEmployeeRoleService updateEmployeeRoleService;

    private UUID employeeId;
    private VeterinaryEmployee existingEmployee;
    private VeterinaryEmployee updatedEmployee;

    @BeforeEach
    void setUp() {
        employeeId = UUID.randomUUID();

        existingEmployee = VeterinaryEmployee.builder()
                .id(employeeId)
                .veterinaryId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .role(EmployeeRole.VETERINARIAN)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        updatedEmployee = existingEmployee.toBuilder()
                .role(EmployeeRole.ADMIN)
                .build();
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
}
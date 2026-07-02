package com.MyAnimaLog.Veterinary.application.services;

import com.MyAnimaLog.Veterinary.application.dto.ActivateEmployeeResponse;
import com.MyAnimaLog.Veterinary.application.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.domain.exceptions.EmployeeNotFoundException;
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
class ActivateEmployeeServiceTest {

    @Mock
    private VeterinaryEmployeeRepositoryPort employeeRepositoryPort;

    @InjectMocks
    private ActivateEmployeeService activateEmployeeService;

    private UUID employeeId;
    private VeterinaryEmployee inactiveEmployee;
    private VeterinaryEmployee activatedEmployee;

    @BeforeEach
    void setUp() {
        employeeId = UUID.randomUUID();

        inactiveEmployee = VeterinaryEmployee.builder()
                .id(employeeId)
                .veterinaryId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .role(EmployeeRole.VETERINARIAN)
                .active(false)
                .createdAt(LocalDateTime.now())
                .build();

        activatedEmployee = inactiveEmployee.toBuilder()
                .active(true)
                .build();
    }

    @Test
    void activate_shouldReturnResponse_withActiveTrue() {
        when(employeeRepositoryPort.findById(employeeId)).thenReturn(Optional.of(inactiveEmployee));
        when(employeeRepositoryPort.save(any(VeterinaryEmployee.class))).thenReturn(activatedEmployee);

        ActivateEmployeeResponse response = activateEmployeeService.activate(employeeId);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(employeeId);
        assertThat(response.getActive()).isTrue();
        assertThat(response.getRole()).isEqualTo(EmployeeRole.VETERINARIAN);
    }

    @Test
    void activate_shouldThrowEmployeeNotFoundException_whenEmployeeDoesNotExist() {
        when(employeeRepositoryPort.findById(employeeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                activateEmployeeService.activate(employeeId)
        ).isInstanceOf(EmployeeNotFoundException.class);
    }

    @Test
    void activate_shouldCallSave_withActiveTrue() {
        when(employeeRepositoryPort.findById(employeeId)).thenReturn(Optional.of(inactiveEmployee));
        when(employeeRepositoryPort.save(any(VeterinaryEmployee.class))).thenReturn(activatedEmployee);

        activateEmployeeService.activate(employeeId);

        verify(employeeRepositoryPort, times(1)).save(argThat(VeterinaryEmployee::getActive));
    }

    @Test
    void activate_shouldCallSave_once() {
        when(employeeRepositoryPort.findById(employeeId)).thenReturn(Optional.of(inactiveEmployee));
        when(employeeRepositoryPort.save(any(VeterinaryEmployee.class))).thenReturn(activatedEmployee);

        activateEmployeeService.activate(employeeId);

        verify(employeeRepositoryPort, times(1)).save(any(VeterinaryEmployee.class));
    }

    @Test
    void activate_shouldNeverCallSave_whenEmployeeNotFound() {
        when(employeeRepositoryPort.findById(employeeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                activateEmployeeService.activate(employeeId)
        ).isInstanceOf(EmployeeNotFoundException.class);

        verify(employeeRepositoryPort, never()).save(any());
    }
}
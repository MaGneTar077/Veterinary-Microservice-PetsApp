package com.MyAnimaLog.Veterinary.application.services;

import com.MyAnimaLog.Veterinary.application.dto.DeActivateEmployeeResponse;
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
class DeActivateEmployeeServiceTest {

    @Mock
    private VeterinaryEmployeeRepositoryPort employeeRepositoryPort;

    @InjectMocks
    private DeActivateEmployeeService deActivateEmployeeService;

    private UUID employeeId;
    private VeterinaryEmployee activeEmployee;
    private VeterinaryEmployee deactivatedEmployee;

    @BeforeEach
    void setUp() {
        employeeId = UUID.randomUUID();

        activeEmployee = VeterinaryEmployee.builder()
                .id(employeeId)
                .veterinaryId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .role(EmployeeRole.ADMIN)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        deactivatedEmployee = activeEmployee.toBuilder()
                .active(false)
                .build();
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
}
package com.MyAnimaLog.Veterinary.application.services;

import com.MyAnimaLog.Veterinary.application.dto.CreateEmployeeRequest;
import com.MyAnimaLog.Veterinary.application.dto.CreateEmployeeResponse;
import com.MyAnimaLog.Veterinary.application.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.application.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.domain.exceptions.EmployeeAlreadyExistsException;
import com.MyAnimaLog.Veterinary.domain.exceptions.InvalidEmployeeRoleException;
import com.MyAnimaLog.Veterinary.domain.exceptions.VeterinaryNotActiveException;
import com.MyAnimaLog.Veterinary.domain.exceptions.VeterinaryNotFoundException;
import com.MyAnimaLog.Veterinary.domain.model.Veterinary;
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
class CreateEmployeeServiceTest {

    @Mock
    private VeterinaryRepositoryPort veterinaryRepositoryPort;

    @Mock
    private VeterinaryEmployeeRepositoryPort employeeRepositoryPort;

    @InjectMocks
    private CreateEmployeeService createEmployeeService;

    private UUID veterinaryId;
    private UUID userId;
    private Veterinary activeVeterinary;
    private Veterinary inactiveVeterinary;
    private VeterinaryEmployee savedEmployee;
    private CreateEmployeeRequest validRequest;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();
        userId = UUID.randomUUID();

        activeVeterinary = Veterinary.builder()
                .id(veterinaryId)
                .name("Clínica El Bosque")
                .city("Cartagena")
                .email("elbosque@veterinaria.com")
                .tenantId(UUID.randomUUID().toString())
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        inactiveVeterinary = activeVeterinary.toBuilder()
                .active(false)
                .build();

        savedEmployee = VeterinaryEmployee.builder()
                .id(UUID.randomUUID())
                .veterinaryId(veterinaryId)
                .userId(userId)
                .role(EmployeeRole.VETERINARIAN)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        validRequest = CreateEmployeeRequest.builder()
                .veterinaryId(veterinaryId)
                .userId(userId)
                .role(EmployeeRole.VETERINARIAN)
                .build();
    }

    @Test
    void create_shouldReturnResponse_whenRequestIsValid() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(activeVeterinary));
        when(employeeRepositoryPort.existsByVeterinaryIdAndUserId(veterinaryId, userId)).thenReturn(false);
        when(employeeRepositoryPort.save(any(VeterinaryEmployee.class))).thenReturn(savedEmployee);

        CreateEmployeeResponse response = createEmployeeService.create(validRequest);

        assertThat(response).isNotNull();
        assertThat(response.getVeterinaryId()).isEqualTo(veterinaryId);
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getRole()).isEqualTo(EmployeeRole.VETERINARIAN);
        assertThat(response.getActive()).isTrue();
        assertThat(response.getCreatedAt()).isNotNull();
    }

    @Test
    void create_shouldThrowInvalidEmployeeRoleException_whenRoleIsNull() {
        validRequest.setRole(null);

        assertThatThrownBy(() ->
                createEmployeeService.create(validRequest)
        ).isInstanceOf(InvalidEmployeeRoleException.class);
    }

    @Test
    void create_shouldThrowVeterinaryNotFoundException_whenVeterinaryDoesNotExist() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                createEmployeeService.create(validRequest)
        ).isInstanceOf(VeterinaryNotFoundException.class);
    }

    @Test
    void create_shouldThrowVeterinaryNotActiveException_whenVeterinaryIsInactive() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(inactiveVeterinary));

        assertThatThrownBy(() ->
                createEmployeeService.create(validRequest)
        ).isInstanceOf(VeterinaryNotActiveException.class);
    }

    @Test
    void create_shouldThrowEmployeeAlreadyExistsException_whenUserAlreadyRegistered() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(activeVeterinary));
        when(employeeRepositoryPort.existsByVeterinaryIdAndUserId(veterinaryId, userId)).thenReturn(true);

        assertThatThrownBy(() ->
                createEmployeeService.create(validRequest)
        ).isInstanceOf(EmployeeAlreadyExistsException.class);
    }

    @Test
    void create_shouldCallSave_once() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(activeVeterinary));
        when(employeeRepositoryPort.existsByVeterinaryIdAndUserId(veterinaryId, userId)).thenReturn(false);
        when(employeeRepositoryPort.save(any(VeterinaryEmployee.class))).thenReturn(savedEmployee);

        createEmployeeService.create(validRequest);

        verify(employeeRepositoryPort, times(1)).save(any(VeterinaryEmployee.class));
    }

    @Test
    void create_shouldNeverCallSave_whenRoleIsNull() {
        validRequest.setRole(null);

        assertThatThrownBy(() ->
                createEmployeeService.create(validRequest)
        ).isInstanceOf(InvalidEmployeeRoleException.class);

        verify(employeeRepositoryPort, never()).save(any());
    }

    @Test
    void create_shouldNeverCallSave_whenVeterinaryNotFound() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                createEmployeeService.create(validRequest)
        ).isInstanceOf(VeterinaryNotFoundException.class);

        verify(employeeRepositoryPort, never()).save(any());
    }

    @Test
    void create_shouldNeverCallSave_whenVeterinaryIsInactive() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(inactiveVeterinary));

        assertThatThrownBy(() ->
                createEmployeeService.create(validRequest)
        ).isInstanceOf(VeterinaryNotActiveException.class);

        verify(employeeRepositoryPort, never()).save(any());
    }
}
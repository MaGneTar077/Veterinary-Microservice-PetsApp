package com.MyAnimaLog.Veterinary.application.services;

import com.MyAnimaLog.Veterinary.application.dto.RegisterVeterinaryRequest;
import com.MyAnimaLog.Veterinary.application.dto.RegisterVeterinaryResponse;
import com.MyAnimaLog.Veterinary.application.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.exceptions.InvalidVeterinaryEmailException;
import com.MyAnimaLog.Veterinary.domain.exceptions.InvalidVeterinaryNameException;
import com.MyAnimaLog.Veterinary.domain.exceptions.VeterinaryAlreadyExistsException;
import com.MyAnimaLog.Veterinary.domain.exceptions.VeterinaryEmailAlreadyExistsException;
import com.MyAnimaLog.Veterinary.domain.model.Veterinary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterVeterinaryServiceTest {

    @Mock
    private VeterinaryRepositoryPort veterinaryRepositoryPort;

    @InjectMocks
    private RegisterVeterinaryService registerVeterinaryService;

    private RegisterVeterinaryRequest validRequest;
    private Veterinary savedVeterinary;

    @BeforeEach

    void setUp() {
        validRequest = RegisterVeterinaryRequest.builder()
                .name("Clínica El Bosque")
                .city("Cartagena")
                .phone("3001234567")
                .email("elbosque@veterinaria.com")
                .build();

        savedVeterinary = Veterinary.builder()
                .id(UUID.randomUUID())
                .name("Clínica El Bosque")
                .city("Cartagena")
                .phone("3001234567")
                .email("elbosque@veterinaria.com")
                .tenantId(UUID.randomUUID().toString())
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void registerVeterinary_shouldReturnResponse_whenRequestIsValid() {
        when(veterinaryRepositoryPort.existsByName(any())).thenReturn(false);
        when(veterinaryRepositoryPort.existsByEmail(any())).thenReturn(false);
        when(veterinaryRepositoryPort.save(any(Veterinary.class))).thenReturn(savedVeterinary);

        RegisterVeterinaryResponse response = registerVeterinaryService.registerVeterinary(validRequest);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Clínica El Bosque");
        assertThat(response.getEmail()).isEqualTo("elbosque@veterinaria.com");
        assertThat(response.getActive()).isTrue();
        assertThat(response.getTenantId()).isNotNull();
        assertThat(response.getCreatedAt()).isNotNull();
    }

    @Test
    void registerVeterinary_shouldThrowInvalidVeterinaryNameException_whenNameIsNull() {
        validRequest.setName(null);

        assertThatThrownBy(() ->
                registerVeterinaryService.registerVeterinary(validRequest)
        ).isInstanceOf(InvalidVeterinaryNameException.class);
    }

    @Test
    void registerVeterinary_shouldThrowInvalidVeterinaryNameException_whenNameIsBlank() {
        validRequest.setName("   ");

        assertThatThrownBy(() ->
                registerVeterinaryService.registerVeterinary(validRequest)
        ).isInstanceOf(InvalidVeterinaryNameException.class);
    }

    @Test
    void registerVeterinary_shouldThrowInvalidVeterinaryEmailException_whenEmailIsNull() {
        validRequest.setEmail(null);

        assertThatThrownBy(() ->
                registerVeterinaryService.registerVeterinary(validRequest)
        ).isInstanceOf(InvalidVeterinaryEmailException.class);
    }

    @Test
    void registerVeterinary_shouldThrowInvalidVeterinaryEmailException_whenEmailIsInvalid() {
        validRequest.setEmail("esto-no-es-un-email");

        assertThatThrownBy(() ->
                registerVeterinaryService.registerVeterinary(validRequest)
        ).isInstanceOf(InvalidVeterinaryEmailException.class);
    }

    @Test
    void registerVeterinary_shouldThrowVeterinaryAlreadyExistsException_whenNameAlreadyExists() {
        when(veterinaryRepositoryPort.existsByName(any())).thenReturn(true);

        assertThatThrownBy(() ->
                registerVeterinaryService.registerVeterinary(validRequest)
        ).isInstanceOf(VeterinaryAlreadyExistsException.class);
    }

    @Test
    void registerVeterinary_shouldThrowVeterinaryEmailAlreadyExistsException_whenEmailAlreadyExists() {
        when(veterinaryRepositoryPort.existsByName(any())).thenReturn(false);
        when(veterinaryRepositoryPort.existsByEmail(any())).thenReturn(true);

        assertThatThrownBy(() ->
                registerVeterinaryService.registerVeterinary(validRequest)
        ).isInstanceOf(VeterinaryEmailAlreadyExistsException.class);
    }

    @Test
    void registerVeterinary_shouldCallSave_once() {
        when(veterinaryRepositoryPort.existsByName(any())).thenReturn(false);
        when(veterinaryRepositoryPort.existsByEmail(any())).thenReturn(false);
        when(veterinaryRepositoryPort.save(any(Veterinary.class))).thenReturn(savedVeterinary);

        registerVeterinaryService.registerVeterinary(validRequest);

        verify(veterinaryRepositoryPort, times(1)).save(any(Veterinary.class));
    }

    @Test
    void registerVeterinary_shouldNeverCallSave_whenValidationFails() {
        validRequest.setName(null);

        assertThatThrownBy(() ->
                registerVeterinaryService.registerVeterinary(validRequest)
        ).isInstanceOf(InvalidVeterinaryNameException.class);

        verify(veterinaryRepositoryPort, never()).save(any());
    }
}
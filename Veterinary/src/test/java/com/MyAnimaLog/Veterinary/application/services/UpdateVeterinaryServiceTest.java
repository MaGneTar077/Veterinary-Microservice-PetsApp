package com.MyAnimaLog.Veterinary.application.services;

import com.MyAnimaLog.Veterinary.application.dto.UpdateVeterinaryRequest;
import com.MyAnimaLog.Veterinary.application.dto.UpdateVeterinaryResponse;
import com.MyAnimaLog.Veterinary.application.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.exceptions.InvalidVeterinaryEmailException;
import com.MyAnimaLog.Veterinary.domain.exceptions.InvalidVeterinaryNameException;
import com.MyAnimaLog.Veterinary.domain.exceptions.VeterinaryEmailAlreadyExistsException;
import com.MyAnimaLog.Veterinary.domain.exceptions.VeterinaryNotFoundException;
import com.MyAnimaLog.Veterinary.domain.model.Veterinary;
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
class UpdateVeterinaryServiceTest {

    @Mock
    private VeterinaryRepositoryPort veterinaryRepositoryPort;

    @InjectMocks
    private UpdateVeterinaryService updateVeterinaryService;

    private UUID veterinaryId;
    private Veterinary existingVeterinary;
    private Veterinary updatedVeterinary;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();

        existingVeterinary = Veterinary.builder()
                .id(veterinaryId)
                .name("Clínica El Bosque")
                .city("Cartagena")
                .phone("3001234567")
                .email("elbosque@veterinaria.com")
                .tenantId(UUID.randomUUID().toString())
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        updatedVeterinary = existingVeterinary.toBuilder()
                .name("Clínica El Bosque Actualizada")
                .phone("3009999999")
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void update_shouldReturnResponse_whenRequestIsValid() {
        UpdateVeterinaryRequest request = UpdateVeterinaryRequest.builder()
                .name("Clínica El Bosque Actualizada")
                .phone("3009999999")
                .build();

        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(existingVeterinary));
        when(veterinaryRepositoryPort.save(any(Veterinary.class))).thenReturn(updatedVeterinary);

        UpdateVeterinaryResponse response = updateVeterinaryService.update(veterinaryId, request);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Clínica El Bosque Actualizada");
        assertThat(response.getPhone()).isEqualTo("3009999999");
        assertThat(response.getUpdatedAt()).isNotNull();
    }

    @Test
    void update_shouldKeepExistingValues_whenFieldsAreNull() {
        UpdateVeterinaryRequest request = UpdateVeterinaryRequest.builder().build();

        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(existingVeterinary));
        when(veterinaryRepositoryPort.save(any(Veterinary.class))).thenReturn(updatedVeterinary);

        updateVeterinaryService.update(veterinaryId, request);

        verify(veterinaryRepositoryPort, times(1)).save(argThat(v ->
                v.getName().equals("Clínica El Bosque") &&
                        v.getEmail().equals("elbosque@veterinaria.com")
        ));
    }

    @Test
    void update_shouldThrowVeterinaryNotFoundException_whenVeterinaryDoesNotExist() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                updateVeterinaryService.update(veterinaryId, UpdateVeterinaryRequest.builder().build())
        ).isInstanceOf(VeterinaryNotFoundException.class);
    }

    @Test
    void update_shouldThrowInvalidVeterinaryNameException_whenNameIsBlank() {
        UpdateVeterinaryRequest request = UpdateVeterinaryRequest.builder()
                .name("   ")
                .build();

        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(existingVeterinary));

        assertThatThrownBy(() ->
                updateVeterinaryService.update(veterinaryId, request)
        ).isInstanceOf(InvalidVeterinaryNameException.class);
    }

    @Test
    void update_shouldThrowInvalidVeterinaryEmailException_whenEmailIsInvalid() {
        UpdateVeterinaryRequest request = UpdateVeterinaryRequest.builder()
                .email("esto-no-es-email")
                .build();

        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(existingVeterinary));

        assertThatThrownBy(() ->
                updateVeterinaryService.update(veterinaryId, request)
        ).isInstanceOf(InvalidVeterinaryEmailException.class);
    }

    @Test
    void update_shouldThrowInvalidVeterinaryEmailException_whenEmailIsBlank() {
        UpdateVeterinaryRequest request = UpdateVeterinaryRequest.builder()
                .email("   ")
                .build();

        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(existingVeterinary));

        assertThatThrownBy(() ->
                updateVeterinaryService.update(veterinaryId, request)
        ).isInstanceOf(InvalidVeterinaryEmailException.class);
    }

    @Test
    void update_shouldThrowVeterinaryEmailAlreadyExistsException_whenEmailBelongsToAnother() {
        UpdateVeterinaryRequest request = UpdateVeterinaryRequest.builder()
                .email("otro@veterinaria.com")
                .build();

        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(existingVeterinary));
        when(veterinaryRepositoryPort.existsByEmailAndIdNot("otro@veterinaria.com", veterinaryId))
                .thenReturn(true);

        assertThatThrownBy(() ->
                updateVeterinaryService.update(veterinaryId, request)
        ).isInstanceOf(VeterinaryEmailAlreadyExistsException.class);
    }

    @Test
    void update_shouldNotThrowEmailException_whenEmailBelongsToSameVeterinary() {
        UpdateVeterinaryRequest request = UpdateVeterinaryRequest.builder()
                .email("elbosque@veterinaria.com")
                .build();

        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(existingVeterinary));
        when(veterinaryRepositoryPort.existsByEmailAndIdNot("elbosque@veterinaria.com", veterinaryId))
                .thenReturn(false);
        when(veterinaryRepositoryPort.save(any(Veterinary.class))).thenReturn(updatedVeterinary);

        assertThatNoException().isThrownBy(() ->
                updateVeterinaryService.update(veterinaryId, request)
        );
    }

    @Test
    void update_shouldCallSave_once() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(existingVeterinary));
        when(veterinaryRepositoryPort.save(any(Veterinary.class))).thenReturn(updatedVeterinary);

        updateVeterinaryService.update(veterinaryId, UpdateVeterinaryRequest.builder().build());

        verify(veterinaryRepositoryPort, times(1)).save(any(Veterinary.class));
    }

    @Test
    void update_shouldNeverCallSave_whenVeterinaryNotFound() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                updateVeterinaryService.update(veterinaryId, UpdateVeterinaryRequest.builder().build())
        ).isInstanceOf(VeterinaryNotFoundException.class);

        verify(veterinaryRepositoryPort, never()).save(any());
    }
}
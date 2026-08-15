package com.MyAnimaLog.Veterinary.application.services;

import com.MyAnimaLog.Veterinary.application.dto.ActivateVeterinaryResponse;
import com.MyAnimaLog.Veterinary.application.ports.out.VeterinaryRepositoryPort;
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
class ActivateVeterinaryServiceTest {

    @Mock
    private VeterinaryRepositoryPort veterinaryRepositoryPort;

    @InjectMocks
    private ActivateVeterinaryService activateVeterinaryService;

    private UUID veterinaryId;
    private Veterinary inactiveVeterinary;
    private Veterinary activatedVeterinary;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();

        inactiveVeterinary = Veterinary.builder()
                .id(veterinaryId)
                .name("Clínica El Bosque")
                .city("Cartagena")
                .phone("3001234567")
                .email("elbosque@veterinaria.com")
                .tenantId(UUID.randomUUID().toString())
                .active(false)
                .createdAt(LocalDateTime.now())
                .build();

        activatedVeterinary = inactiveVeterinary.toBuilder()
                .active(true)
                .build();
    }

    @Test
    void activate_shouldReturnResponse_withActiveTrue() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(inactiveVeterinary));
        when(veterinaryRepositoryPort.save(any(Veterinary.class))).thenReturn(activatedVeterinary);

        ActivateVeterinaryResponse response = activateVeterinaryService.activate(veterinaryId);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(veterinaryId);
        assertThat(response.getName()).isEqualTo("Clínica El Bosque");
        assertThat(response.getActive()).isTrue();
    }

    @Test
    void activate_shouldThrowVeterinaryNotFoundException_whenVeterinaryDoesNotExist() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                activateVeterinaryService.activate(veterinaryId)
        ).isInstanceOf(VeterinaryNotFoundException.class);
    }

    @Test
    void activate_shouldCallSave_withActiveTrue() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(inactiveVeterinary));
        when(veterinaryRepositoryPort.save(any(Veterinary.class))).thenReturn(activatedVeterinary);

        activateVeterinaryService.activate(veterinaryId);

        verify(veterinaryRepositoryPort, times(1)).save(argThat(Veterinary::getActive));
    }

    @Test
    void activate_shouldCallSave_once() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(inactiveVeterinary));
        when(veterinaryRepositoryPort.save(any(Veterinary.class))).thenReturn(activatedVeterinary);

        activateVeterinaryService.activate(veterinaryId);

        verify(veterinaryRepositoryPort, times(1)).save(any(Veterinary.class));
    }

    @Test
    void activate_shouldNeverCallSave_whenVeterinaryNotFound() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                activateVeterinaryService.activate(veterinaryId)
        ).isInstanceOf(VeterinaryNotFoundException.class);

        verify(veterinaryRepositoryPort, never()).save(any());
    }
}
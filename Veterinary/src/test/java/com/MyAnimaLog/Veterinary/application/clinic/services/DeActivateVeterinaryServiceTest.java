package com.MyAnimaLog.Veterinary.application.clinic.services;

import com.MyAnimaLog.Veterinary.application.clinic.dto.DeActivateVeterinaryResponse;
import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.PlatformAdminRequiredException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VeterinaryNotFoundException;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
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
class DeActivateVeterinaryServiceTest {

    @Mock
    private VeterinaryRepositoryPort veterinaryRepositoryPort;

    @Mock
    private VeterinaryAuthorizationService authorizationService;

    @InjectMocks
    private DeActivateVeterinaryService deActivateVeterinaryService;

    private UUID veterinaryId;
    private Veterinary activeVeterinary;
    private Veterinary deactivatedVeterinary;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();

        activeVeterinary = Veterinary.builder()
                .id(veterinaryId)
                .name("Clínica El Bosque")
                .city("Cartagena")
                .phone("3001234567")
                .email("elbosque@veterinaria.com")
                .tenantId(UUID.randomUUID().toString())
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        deactivatedVeterinary = activeVeterinary.toBuilder()
                .active(false)
                .build();
    }

    @Test
    void deActivate_shouldReturnResponse_withActiveFalse() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(activeVeterinary));
        when(veterinaryRepositoryPort.save(any(Veterinary.class))).thenReturn(deactivatedVeterinary);

        DeActivateVeterinaryResponse response = deActivateVeterinaryService.deActivate(veterinaryId);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(veterinaryId);
        assertThat(response.getName()).isEqualTo("Clínica El Bosque");
        assertThat(response.getActive()).isFalse();
    }

    @Test
    void deActivate_shouldThrowVeterinaryNotFoundException_whenVeterinaryDoesNotExist() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                deActivateVeterinaryService.deActivate(veterinaryId)
        ).isInstanceOf(VeterinaryNotFoundException.class);
    }

    @Test
    void deActivate_shouldCallSave_withActiveFalse() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(activeVeterinary));
        when(veterinaryRepositoryPort.save(any(Veterinary.class))).thenReturn(deactivatedVeterinary);

        deActivateVeterinaryService.deActivate(veterinaryId);

        verify(veterinaryRepositoryPort, times(1)).save(argThat(v -> !v.getActive()));
    }

    @Test
    void deActivate_shouldCallSave_once() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(activeVeterinary));
        when(veterinaryRepositoryPort.save(any(Veterinary.class))).thenReturn(deactivatedVeterinary);

        deActivateVeterinaryService.deActivate(veterinaryId);

        verify(veterinaryRepositoryPort, times(1)).save(any(Veterinary.class));
    }

    @Test
    void deActivate_shouldNeverCallSave_whenVeterinaryNotFound() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                deActivateVeterinaryService.deActivate(veterinaryId)
        ).isInstanceOf(VeterinaryNotFoundException.class);

        verify(veterinaryRepositoryPort, never()).save(any());
    }

    @Test
    void deActivate_shouldThrowPlatformAdminRequiredException_whenCallerIsNotPlatformAdmin() {
        doThrow(new PlatformAdminRequiredException()).when(authorizationService).requirePlatformAdmin();

        assertThatThrownBy(() ->
                deActivateVeterinaryService.deActivate(veterinaryId)
        ).isInstanceOf(PlatformAdminRequiredException.class);

        verify(veterinaryRepositoryPort, never()).save(any());
    }
}
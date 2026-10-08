package com.MyAnimaLog.Veterinary.application.clinic.services;

import com.MyAnimaLog.Veterinary.application.clinic.dto.SuspendVeterinaryResponse;
import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.InvalidVeterinaryStatusTransitionException;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.PlatformAdminRequiredException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VeterinaryNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SuspendVeterinaryServiceTest {

    @Mock
    private VeterinaryRepositoryPort veterinaryRepositoryPort;

    @Mock
    private VeterinaryAuthorizationService authorizationService;

    @InjectMocks
    private SuspendVeterinaryService suspendVeterinaryService;

    private UUID veterinaryId;
    private Veterinary activeVeterinary;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();
        activeVeterinary = Veterinary.builder()
                .id(veterinaryId)
                .name("Clínica El Bosque")
                .status(VeterinaryStatus.ACTIVE)
                .active(true)
                .build();
    }

    @Test
    void suspend_shouldSetStatusSuspended_whenCurrentlyActive() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(activeVeterinary));
        when(veterinaryRepositoryPort.save(any(Veterinary.class))).thenAnswer(inv -> inv.getArgument(0));

        SuspendVeterinaryResponse response = suspendVeterinaryService.suspend(veterinaryId);

        assertThat(response.getStatus()).isEqualTo(VeterinaryStatus.SUSPENDED);
        assertThat(response.getActive()).isFalse();
    }

    @Test
    void suspend_shouldThrowVeterinaryNotFoundException_whenVeterinaryDoesNotExist() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> suspendVeterinaryService.suspend(veterinaryId))
                .isInstanceOf(VeterinaryNotFoundException.class);
    }

    @Test
    void suspend_shouldThrowInvalidTransition_whenNotCurrentlyActive() {
        Veterinary pending = activeVeterinary.toBuilder().status(VeterinaryStatus.PENDING_DOCUMENTS).build();
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> suspendVeterinaryService.suspend(veterinaryId))
                .isInstanceOf(InvalidVeterinaryStatusTransitionException.class);

        verify(veterinaryRepositoryPort, never()).save(any());
    }

    @Test
    void suspend_shouldThrowPlatformAdminRequiredException_whenCallerIsNotPlatformAdmin() {
        doThrow(new PlatformAdminRequiredException()).when(authorizationService).requirePlatformAdmin();

        assertThatThrownBy(() -> suspendVeterinaryService.suspend(veterinaryId))
                .isInstanceOf(PlatformAdminRequiredException.class);

        verify(veterinaryRepositoryPort, never()).save(any());
    }
}

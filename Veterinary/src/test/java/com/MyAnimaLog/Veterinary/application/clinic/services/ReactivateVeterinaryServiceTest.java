package com.MyAnimaLog.Veterinary.application.clinic.services;

import com.MyAnimaLog.Veterinary.application.clinic.dto.ReactivateVeterinaryResponse;
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

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReactivateVeterinaryServiceTest {

    @Mock
    private VeterinaryRepositoryPort veterinaryRepositoryPort;

    @Mock
    private VeterinaryAuthorizationService authorizationService;

    @InjectMocks
    private ReactivateVeterinaryService reactivateVeterinaryService;

    private UUID veterinaryId;
    private LocalDateTime originalApprovedAt;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();
        originalApprovedAt = LocalDateTime.now().minusDays(30);
        lenient().when(veterinaryRepositoryPort.save(any(Veterinary.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void reactivate_shouldSetStatusActive_whenCurrentlySuspended_andKeepApprovedAt() {
        Veterinary suspended = Veterinary.builder()
                .id(veterinaryId).status(VeterinaryStatus.SUSPENDED).active(false)
                .approvedAt(originalApprovedAt).build();
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(suspended));

        ReactivateVeterinaryResponse response = reactivateVeterinaryService.reactivate(veterinaryId);

        assertThat(response.getStatus()).isEqualTo(VeterinaryStatus.ACTIVE);
        assertThat(response.getActive()).isTrue();
        assertThat(response.getApprovedAt()).isEqualTo(originalApprovedAt);
    }

    @Test
    void reactivate_shouldSetApprovedAt_whenComingFromPendingDocuments() {
        Veterinary pending = Veterinary.builder()
                .id(veterinaryId).status(VeterinaryStatus.PENDING_DOCUMENTS).active(false)
                .build();
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(pending));

        ReactivateVeterinaryResponse response = reactivateVeterinaryService.reactivate(veterinaryId);

        assertThat(response.getStatus()).isEqualTo(VeterinaryStatus.ACTIVE);
        assertThat(response.getActive()).isTrue();
        assertThat(response.getApprovedAt()).isNotNull();
    }

    @Test
    void reactivate_shouldSetApprovedAt_whenComingFromUnderReview() {
        Veterinary underReview = Veterinary.builder()
                .id(veterinaryId).status(VeterinaryStatus.UNDER_REVIEW).active(false)
                .build();
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(underReview));

        ReactivateVeterinaryResponse response = reactivateVeterinaryService.reactivate(veterinaryId);

        assertThat(response.getApprovedAt()).isNotNull();
    }

    @Test
    void reactivate_shouldThrowInvalidTransition_whenAlreadyActive() {
        Veterinary active = Veterinary.builder().id(veterinaryId).status(VeterinaryStatus.ACTIVE).active(true).build();
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(active));

        assertThatThrownBy(() -> reactivateVeterinaryService.reactivate(veterinaryId))
                .isInstanceOf(InvalidVeterinaryStatusTransitionException.class);

        verify(veterinaryRepositoryPort, never()).save(any());
    }

    @Test
    void reactivate_shouldThrowInvalidTransition_whenRejected() {
        Veterinary rejected = Veterinary.builder().id(veterinaryId).status(VeterinaryStatus.REJECTED).active(false).build();
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(rejected));

        assertThatThrownBy(() -> reactivateVeterinaryService.reactivate(veterinaryId))
                .isInstanceOf(InvalidVeterinaryStatusTransitionException.class);

        verify(veterinaryRepositoryPort, never()).save(any());
    }

    @Test
    void reactivate_shouldThrowVeterinaryNotFoundException_whenVeterinaryDoesNotExist() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reactivateVeterinaryService.reactivate(veterinaryId))
                .isInstanceOf(VeterinaryNotFoundException.class);
    }

    @Test
    void reactivate_shouldThrowPlatformAdminRequiredException_whenCallerIsNotPlatformAdmin() {
        doThrow(new PlatformAdminRequiredException()).when(authorizationService).requirePlatformAdmin();

        assertThatThrownBy(() -> reactivateVeterinaryService.reactivate(veterinaryId))
                .isInstanceOf(PlatformAdminRequiredException.class);

        verify(veterinaryRepositoryPort, never()).save(any());
    }
}

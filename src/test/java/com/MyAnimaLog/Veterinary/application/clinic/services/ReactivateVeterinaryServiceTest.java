package com.MyAnimaLog.Veterinary.application.clinic.services;

import com.MyAnimaLog.Veterinary.application.clinic.dto.ReactivateVeterinaryResponse;
import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.dto.AuthenticatedUser;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.AuthenticatedUserPort;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.EventPublisherPort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.CannotManageOwnClinicException;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.ClinicNotSuspendedException;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.PlatformAdminRequiredException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VeterinaryNotFoundException;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.domain.staff.model.VeterinaryEmployee;
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
    private VeterinaryEmployeeRepositoryPort employeeRepositoryPort;

    @Mock
    private VeterinaryAuthorizationService authorizationService;

    @Mock
    private AuthenticatedUserPort authenticatedUserPort;

    @Mock
    private EventPublisherPort eventPublisherPort;

    @InjectMocks
    private ReactivateVeterinaryService reactivateVeterinaryService;

    private UUID veterinaryId;
    private UUID adminUserId;
    private LocalDateTime originalApprovedAt;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();
        adminUserId = UUID.randomUUID();
        originalApprovedAt = LocalDateTime.now().minusDays(30);
        lenient().when(veterinaryRepositoryPort.save(any(Veterinary.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(authenticatedUserPort.current())
                .thenReturn(new AuthenticatedUser(adminUserId, "admin@example.com", true, true, Optional.empty()));
        lenient().when(employeeRepositoryPort.existsByVeterinaryIdAndUserId(veterinaryId, adminUserId)).thenReturn(false);
        lenient().when(veterinaryRepositoryPort.claimStatusTransition(veterinaryId, VeterinaryStatus.SUSPENDED, VeterinaryStatus.ACTIVE))
                .thenReturn(true);
    }

    @Test
    void reactivate_shouldSetStatusActive_whenCurrentlySuspended_andKeepApprovedAt_andClearSuspensionFields() {
        Veterinary suspended = Veterinary.builder()
                .id(veterinaryId).status(VeterinaryStatus.SUSPENDED).active(false)
                .approvedAt(originalApprovedAt)
                .suspendedAt(LocalDateTime.now().minusDays(1)).suspensionReason("some reason")
                .build();
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(suspended));

        ReactivateVeterinaryResponse response = reactivateVeterinaryService.reactivate(veterinaryId);

        assertThat(response.getStatus()).isEqualTo(VeterinaryStatus.ACTIVE);
        assertThat(response.getActive()).isTrue();
        assertThat(response.getApprovedAt()).isEqualTo(originalApprovedAt);

        verify(veterinaryRepositoryPort).save(argThat(v ->
                v.getSuspendedAt() == null && v.getSuspensionReason() == null));
    }

    @Test
    void reactivate_shouldPublishEventToOwner_whenClinicHasAnActiveOwner() {
        UUID ownerUserId = UUID.randomUUID();
        VeterinaryEmployee owner = VeterinaryEmployee.builder()
                .id(UUID.randomUUID()).veterinaryId(veterinaryId).userId(ownerUserId).role(EmployeeRole.OWNER).active(true).build();
        Veterinary suspended = Veterinary.builder().id(veterinaryId).status(VeterinaryStatus.SUSPENDED).active(false).build();
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(suspended));
        when(employeeRepositoryPort.findByVeterinaryIdAndRoleAndActiveTrue(veterinaryId, EmployeeRole.OWNER))
                .thenReturn(Optional.of(owner));

        reactivateVeterinaryService.reactivate(veterinaryId);

        verify(eventPublisherPort, times(1)).publish(any());
    }

    @Test
    void reactivate_shouldThrowClinicNotSuspended_whenPendingDocuments() {
        Veterinary pending = Veterinary.builder()
                .id(veterinaryId).status(VeterinaryStatus.PENDING_DOCUMENTS).active(false)
                .build();
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> reactivateVeterinaryService.reactivate(veterinaryId))
                .isInstanceOf(ClinicNotSuspendedException.class);

        verify(veterinaryRepositoryPort, never()).save(any());
    }

    @Test
    void reactivate_shouldThrowClinicNotSuspended_whenUnderReview() {
        Veterinary underReview = Veterinary.builder()
                .id(veterinaryId).status(VeterinaryStatus.UNDER_REVIEW).active(false)
                .build();
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(underReview));

        assertThatThrownBy(() -> reactivateVeterinaryService.reactivate(veterinaryId))
                .isInstanceOf(ClinicNotSuspendedException.class);

        verify(veterinaryRepositoryPort, never()).save(any());
    }

    @Test
    void reactivate_shouldThrowClinicNotSuspended_whenAlreadyActive() {
        Veterinary active = Veterinary.builder().id(veterinaryId).status(VeterinaryStatus.ACTIVE).active(true).build();
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(active));

        assertThatThrownBy(() -> reactivateVeterinaryService.reactivate(veterinaryId))
                .isInstanceOf(ClinicNotSuspendedException.class);

        verify(veterinaryRepositoryPort, never()).save(any());
    }

    @Test
    void reactivate_shouldThrowClinicNotSuspended_whenRejected() {
        Veterinary rejected = Veterinary.builder().id(veterinaryId).status(VeterinaryStatus.REJECTED).active(false).build();
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(rejected));

        assertThatThrownBy(() -> reactivateVeterinaryService.reactivate(veterinaryId))
                .isInstanceOf(ClinicNotSuspendedException.class);

        verify(veterinaryRepositoryPort, never()).save(any());
    }

    @Test
    void reactivate_shouldThrowClinicNotSuspended_whenConditionalUpdateLosesTheRace() {
        Veterinary suspended = Veterinary.builder().id(veterinaryId).status(VeterinaryStatus.SUSPENDED).active(false).build();
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(suspended));
        when(veterinaryRepositoryPort.claimStatusTransition(veterinaryId, VeterinaryStatus.SUSPENDED, VeterinaryStatus.ACTIVE))
                .thenReturn(false);

        assertThatThrownBy(() -> reactivateVeterinaryService.reactivate(veterinaryId))
                .isInstanceOf(ClinicNotSuspendedException.class);

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

    @Test
    void reactivate_shouldThrowCannotManageOwnClinic_whenCallerIsEmployeeOfTheClinic() {
        Veterinary suspended = Veterinary.builder().id(veterinaryId).status(VeterinaryStatus.SUSPENDED).active(false).build();
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(suspended));
        when(employeeRepositoryPort.existsByVeterinaryIdAndUserId(veterinaryId, adminUserId)).thenReturn(true);

        assertThatThrownBy(() -> reactivateVeterinaryService.reactivate(veterinaryId))
                .isInstanceOf(CannotManageOwnClinicException.class);

        verify(veterinaryRepositoryPort, never()).save(any());
    }
}

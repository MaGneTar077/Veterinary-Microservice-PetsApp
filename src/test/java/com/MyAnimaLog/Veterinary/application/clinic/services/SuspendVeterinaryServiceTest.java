package com.MyAnimaLog.Veterinary.application.clinic.services;

import com.MyAnimaLog.Veterinary.application.clinic.dto.SuspendVeterinaryRequest;
import com.MyAnimaLog.Veterinary.application.clinic.dto.SuspendVeterinaryResponse;
import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.dto.AuthenticatedUser;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.AuthenticatedUserPort;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.EventPublisherPort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.CannotManageOwnClinicException;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.ClinicNotActiveException;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.InvalidSuspensionReasonException;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SuspendVeterinaryServiceTest {

    private static final String VALID_REASON = "Pago atrasado por mas de 60 dias";

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
    private SuspendVeterinaryService suspendVeterinaryService;

    private UUID veterinaryId;
    private UUID adminUserId;
    private Veterinary activeVeterinary;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(suspendVeterinaryService, "supportEmail", "soporte@myanimalog.com");

        veterinaryId = UUID.randomUUID();
        adminUserId = UUID.randomUUID();
        activeVeterinary = Veterinary.builder()
                .id(veterinaryId)
                .name("Clínica El Bosque")
                .status(VeterinaryStatus.ACTIVE)
                .active(true)
                .build();

        lenient().when(authenticatedUserPort.current())
                .thenReturn(new AuthenticatedUser(adminUserId, "admin@example.com", true, true, Optional.empty()));
        lenient().when(employeeRepositoryPort.existsByVeterinaryIdAndUserId(veterinaryId, adminUserId)).thenReturn(false);
    }

    private SuspendVeterinaryRequest requestWithReason(String reason) {
        return SuspendVeterinaryRequest.builder().reason(reason).build();
    }

    @Test
    void suspend_shouldSetStatusSuspendedAndRecordReason_whenCurrentlyActive() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(activeVeterinary));
        when(veterinaryRepositoryPort.claimStatusTransition(veterinaryId, VeterinaryStatus.ACTIVE, VeterinaryStatus.SUSPENDED))
                .thenReturn(true);
        when(veterinaryRepositoryPort.save(any(Veterinary.class))).thenAnswer(inv -> inv.getArgument(0));

        SuspendVeterinaryResponse response = suspendVeterinaryService.suspend(veterinaryId, requestWithReason(VALID_REASON));

        assertThat(response.getStatus()).isEqualTo(VeterinaryStatus.SUSPENDED);
        assertThat(response.getActive()).isFalse();
        assertThat(response.getSuspensionReason()).isEqualTo(VALID_REASON);
        assertThat(response.getSuspendedAt()).isNotNull();
    }

    @Test
    void suspend_shouldPublishEventToOwner_whenClinicHasAnActiveOwner() {
        UUID ownerUserId = UUID.randomUUID();
        VeterinaryEmployee owner = VeterinaryEmployee.builder()
                .id(UUID.randomUUID()).veterinaryId(veterinaryId).userId(ownerUserId).role(EmployeeRole.OWNER).active(true).build();
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(activeVeterinary));
        when(veterinaryRepositoryPort.claimStatusTransition(veterinaryId, VeterinaryStatus.ACTIVE, VeterinaryStatus.SUSPENDED))
                .thenReturn(true);
        when(veterinaryRepositoryPort.save(any(Veterinary.class))).thenAnswer(inv -> inv.getArgument(0));
        when(employeeRepositoryPort.findByVeterinaryIdAndRoleAndActiveTrue(veterinaryId, EmployeeRole.OWNER))
                .thenReturn(Optional.of(owner));

        suspendVeterinaryService.suspend(veterinaryId, requestWithReason(VALID_REASON));

        verify(eventPublisherPort, times(1)).publish(any());
    }

    @Test
    void suspend_shouldThrowVeterinaryNotFoundException_whenVeterinaryDoesNotExist() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> suspendVeterinaryService.suspend(veterinaryId, requestWithReason(VALID_REASON)))
                .isInstanceOf(VeterinaryNotFoundException.class);
    }

    @Test
    void suspend_shouldThrowClinicNotActive_whenNotCurrentlyActive() {
        Veterinary pending = activeVeterinary.toBuilder().status(VeterinaryStatus.PENDING_DOCUMENTS).build();
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> suspendVeterinaryService.suspend(veterinaryId, requestWithReason(VALID_REASON)))
                .isInstanceOf(ClinicNotActiveException.class);

        verify(veterinaryRepositoryPort, never()).save(any());
    }

    @Test
    void suspend_shouldThrowClinicNotActive_whenConditionalUpdateLosesTheRace() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(activeVeterinary));
        when(veterinaryRepositoryPort.claimStatusTransition(veterinaryId, VeterinaryStatus.ACTIVE, VeterinaryStatus.SUSPENDED))
                .thenReturn(false);

        assertThatThrownBy(() -> suspendVeterinaryService.suspend(veterinaryId, requestWithReason(VALID_REASON)))
                .isInstanceOf(ClinicNotActiveException.class);

        verify(veterinaryRepositoryPort, never()).save(any());
    }

    @Test
    void suspend_shouldThrowPlatformAdminRequiredException_whenCallerIsNotPlatformAdmin() {
        doThrow(new PlatformAdminRequiredException()).when(authorizationService).requirePlatformAdmin();

        assertThatThrownBy(() -> suspendVeterinaryService.suspend(veterinaryId, requestWithReason(VALID_REASON)))
                .isInstanceOf(PlatformAdminRequiredException.class);

        verify(veterinaryRepositoryPort, never()).save(any());
    }

    @Test
    void suspend_shouldThrowCannotManageOwnClinic_whenCallerIsEmployeeOfTheClinic() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(activeVeterinary));
        when(employeeRepositoryPort.existsByVeterinaryIdAndUserId(veterinaryId, adminUserId)).thenReturn(true);

        assertThatThrownBy(() -> suspendVeterinaryService.suspend(veterinaryId, requestWithReason(VALID_REASON)))
                .isInstanceOf(CannotManageOwnClinicException.class);

        verify(veterinaryRepositoryPort, never()).save(any());
    }

    @Test
    void suspend_shouldThrowInvalidSuspensionReason_whenReasonIsNull() {
        assertThatThrownBy(() -> suspendVeterinaryService.suspend(veterinaryId, requestWithReason(null)))
                .isInstanceOf(InvalidSuspensionReasonException.class);

        verify(veterinaryRepositoryPort, never()).findById(any());
    }

    @Test
    void suspend_shouldThrowInvalidSuspensionReason_whenReasonIsTooShort() {
        assertThatThrownBy(() -> suspendVeterinaryService.suspend(veterinaryId, requestWithReason("short")))
                .isInstanceOf(InvalidSuspensionReasonException.class);
    }

    @Test
    void suspend_shouldThrowInvalidSuspensionReason_whenReasonIsTooLong() {
        String tooLong = "a".repeat(501);

        assertThatThrownBy(() -> suspendVeterinaryService.suspend(veterinaryId, requestWithReason(tooLong)))
                .isInstanceOf(InvalidSuspensionReasonException.class);
    }

    @Test
    void suspend_shouldAccept_whenReasonIsExactlyAtBoundaries() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(activeVeterinary));
        when(veterinaryRepositoryPort.claimStatusTransition(veterinaryId, VeterinaryStatus.ACTIVE, VeterinaryStatus.SUSPENDED))
                .thenReturn(true);
        when(veterinaryRepositoryPort.save(any(Veterinary.class))).thenAnswer(inv -> inv.getArgument(0));

        String tenChars = "a".repeat(10);
        assertThat(suspendVeterinaryService.suspend(veterinaryId, requestWithReason(tenChars)).getStatus())
                .isEqualTo(VeterinaryStatus.SUSPENDED);

        String fiveHundredChars = "a".repeat(500);
        assertThat(suspendVeterinaryService.suspend(veterinaryId, requestWithReason(fiveHundredChars)).getStatus())
                .isEqualTo(VeterinaryStatus.SUSPENDED);
    }
}

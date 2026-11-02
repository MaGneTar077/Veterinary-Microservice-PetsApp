package com.MyAnimaLog.Veterinary.application.staff.services;

import com.MyAnimaLog.Veterinary.application.shared.dto.ClinicContext;
import com.MyAnimaLog.Veterinary.application.shared.dto.DomainEvent;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.EventPublisherPort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.application.staff.dto.LeaveVeterinaryResponse;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.shared.enums.VeterinaryEventType;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.NotClinicMemberException;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.OwnerCannotLeaveException;
import com.MyAnimaLog.Veterinary.domain.staff.model.VeterinaryEmployee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LeaveVeterinaryServiceTest {

    @Mock
    private VeterinaryEmployeeRepositoryPort employeeRepositoryPort;

    @Mock
    private VeterinaryAuthorizationService authorizationService;

    @Mock
    private EventPublisherPort eventPublisherPort;

    @InjectMocks
    private LeaveVeterinaryService leaveVeterinaryService;

    private UUID veterinaryId;
    private UUID employeeId;
    private UUID ownerUserId;
    private VeterinaryEmployee me;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();
        employeeId = UUID.randomUUID();
        ownerUserId = UUID.randomUUID();

        me = VeterinaryEmployee.builder()
                .id(employeeId)
                .veterinaryId(veterinaryId)
                .userId(UUID.randomUUID())
                .role(EmployeeRole.VETERINARIAN)
                .active(true)
                .createdAt(LocalDateTime.now().minusMonths(3))
                .build();

        callerIs(EmployeeRole.VETERINARIAN, VeterinaryStatus.ACTIVE);
        when(employeeRepositoryPort.findById(employeeId)).thenReturn(Optional.of(me));
        when(employeeRepositoryPort.save(any(VeterinaryEmployee.class))).thenAnswer(c -> c.getArgument(0));
        when(employeeRepositoryPort.findByVeterinaryIdAndRoleAndActiveTrue(veterinaryId, EmployeeRole.OWNER))
                .thenReturn(Optional.of(VeterinaryEmployee.builder()
                        .id(UUID.randomUUID())
                        .veterinaryId(veterinaryId)
                        .userId(ownerUserId)
                        .role(EmployeeRole.OWNER)
                        .active(true)
                        .build()));
    }

    private void callerIs(EmployeeRole role, VeterinaryStatus clinicStatus) {
        when(authorizationService.requireMember(veterinaryId)).thenReturn(
                new ClinicContext(veterinaryId, employeeId, role, false, clinicStatus));
    }

    @Test
    void leave_shouldDeactivateTheCallerOwnRecordAndStampRemovedAt() {
        LeaveVeterinaryResponse response = leaveVeterinaryService.leave(veterinaryId);

        ArgumentCaptor<VeterinaryEmployee> captor = ArgumentCaptor.forClass(VeterinaryEmployee.class);
        verify(employeeRepositoryPort).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(employeeId);
        assertThat(captor.getValue().getActive()).isFalse();
        assertThat(captor.getValue().getRemovedAt()).isNotNull();
        assertThat(response.getEmployeeId()).isEqualTo(employeeId);
        assertThat(response.getRemovedAt()).isNotNull();
    }

    @Test
    void leave_shouldThrowOwnerCannotLeaveException_whenTheCallerIsTheOwner() {
        callerIs(EmployeeRole.OWNER, VeterinaryStatus.ACTIVE);

        assertThatThrownBy(() -> leaveVeterinaryService.leave(veterinaryId))
                .isInstanceOf(OwnerCannotLeaveException.class);

        verify(employeeRepositoryPort, never()).save(any());
        verify(eventPublisherPort, never()).publish(any());
    }

    @Test
    void leave_shouldWorkOnASuspendedClinic() {
        // Usa requireMember (no requireActiveMember) justamente para esto.
        callerIs(EmployeeRole.ASSISTANT, VeterinaryStatus.SUSPENDED);

        assertThat(leaveVeterinaryService.leave(veterinaryId).getRemovedAt()).isNotNull();

        verify(authorizationService).requireMember(veterinaryId);
        verify(authorizationService, never()).requireActiveMember(veterinaryId);
    }

    @Test
    void leave_shouldTakeTheEmployeeFromTheContext_neverFromTheRequest() {
        leaveVeterinaryService.leave(veterinaryId);

        verify(employeeRepositoryPort).findById(employeeId);
    }

    @Test
    void leave_shouldPublishEmployeeDeactivatedToTheOwner_markedAsVoluntary() {
        leaveVeterinaryService.leave(veterinaryId);

        ArgumentCaptor<DomainEvent> captor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisherPort).publish(captor.capture());
        DomainEvent event = captor.getValue();
        assertThat(event.eventType()).isEqualTo(VeterinaryEventType.EMPLOYEE_DEACTIVATED);
        assertThat(event.userId()).isEqualTo(ownerUserId);
        assertThat(event.payload()).containsEntry("leftVoluntarily", true);
        assertThat(event.payload()).containsEntry("employeeId", employeeId);
    }

    @Test
    void leave_shouldStillSucceed_whenThereIsNoActiveOwnerToNotify() {
        when(employeeRepositoryPort.findByVeterinaryIdAndRoleAndActiveTrue(veterinaryId, EmployeeRole.OWNER))
                .thenReturn(Optional.empty());

        assertThat(leaveVeterinaryService.leave(veterinaryId).getRemovedAt()).isNotNull();

        verify(eventPublisherPort, never()).publish(any());
    }

    @Test
    void leave_shouldThrowNotClinicMember_whenTheCallerWasAlreadyRemoved() {
        doThrow(new NotClinicMemberException()).when(authorizationService).requireMember(veterinaryId);

        assertThatThrownBy(() -> leaveVeterinaryService.leave(veterinaryId))
                .isInstanceOf(NotClinicMemberException.class);

        verify(employeeRepositoryPort, never()).save(any());
    }
}

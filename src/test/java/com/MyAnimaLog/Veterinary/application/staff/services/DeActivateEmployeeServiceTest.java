package com.MyAnimaLog.Veterinary.application.staff.services;

import com.MyAnimaLog.Veterinary.application.shared.dto.ClinicContext;
import com.MyAnimaLog.Veterinary.application.shared.dto.DomainEvent;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.EventPublisherPort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.application.staff.dto.DeActivateEmployeeResponse;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.security.Permission;
import com.MyAnimaLog.Veterinary.domain.shared.enums.VeterinaryEventType;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.ClinicSuspendedException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.InsufficientPermissionException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.NotClinicMemberException;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.CannotModifyOwnerException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.CannotModifyPeerAdminException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.CannotModifySelfException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.EmployeeNotFoundException;
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
class DeActivateEmployeeServiceTest {

    @Mock
    private VeterinaryEmployeeRepositoryPort employeeRepositoryPort;

    @Mock
    private VeterinaryAuthorizationService authorizationService;

    @Mock
    private EventPublisherPort eventPublisherPort;

    @InjectMocks
    private DeActivateEmployeeService deActivateEmployeeService;

    private UUID employeeId;
    private UUID veterinaryId;
    private UUID actingEmployeeId;
    private VeterinaryEmployee target;

    @BeforeEach
    void setUp() {
        employeeId = UUID.randomUUID();
        veterinaryId = UUID.randomUUID();
        actingEmployeeId = UUID.randomUUID();

        target = VeterinaryEmployee.builder()
                .id(employeeId)
                .veterinaryId(veterinaryId)
                .userId(UUID.randomUUID())
                .role(EmployeeRole.ASSISTANT)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(employeeRepositoryPort.findById(employeeId)).thenReturn(Optional.of(target));
        when(employeeRepositoryPort.save(any(VeterinaryEmployee.class))).thenAnswer(c -> c.getArgument(0));
        when(authorizationService.currentClinicId()).thenReturn(Optional.of(veterinaryId));
        actingAs(EmployeeRole.OWNER);
    }

    private void actingAs(EmployeeRole role) {
        when(authorizationService.require(veterinaryId, Permission.STAFF_MANAGE)).thenReturn(
                new ClinicContext(veterinaryId, actingEmployeeId, role, false, VeterinaryStatus.ACTIVE));
    }

    @Test
    void deActivate_shouldDeactivateAndStampRemovedAt_insteadOfDeleting() {
        DeActivateEmployeeResponse response = deActivateEmployeeService.deActivate(veterinaryId, employeeId);

        ArgumentCaptor<VeterinaryEmployee> captor = ArgumentCaptor.forClass(VeterinaryEmployee.class);
        verify(employeeRepositoryPort).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(employeeId);
        assertThat(captor.getValue().getActive()).isFalse();
        assertThat(captor.getValue().getRemovedAt()).isNotNull();
        assertThat(response.getActive()).isFalse();
    }

    @Test
    void deActivate_shouldRequireStaffManage() {
        deActivateEmployeeService.deActivate(veterinaryId, employeeId);

        verify(authorizationService).require(veterinaryId, Permission.STAFF_MANAGE);
    }

    @Test
    void deActivate_shouldWorkWithoutAVeterinaryIdInThePath_forTheDeprecatedRoute() {
        assertThat(deActivateEmployeeService.deActivate(null, employeeId).getActive()).isFalse();

        verify(authorizationService).require(veterinaryId, Permission.STAFF_MANAGE);
    }

    @Test
    void deActivate_shouldThrowEmployeeNotFound_whenTheDeprecatedRouteTargetsAnEmployeeOfAnotherClinic() {
        when(authorizationService.currentClinicId()).thenReturn(Optional.of(UUID.randomUUID()));

        assertThatThrownBy(() -> deActivateEmployeeService.deActivate(null, employeeId))
                .isInstanceOf(EmployeeNotFoundException.class);

        verify(employeeRepositoryPort, never()).save(any());
        verify(authorizationService, never()).require(any(), any());
    }

    @Test
    void deActivate_shouldThrowEmployeeNotFoundException_whenTheEmployeeBelongsToAnotherClinic() {
        assertThatThrownBy(() -> deActivateEmployeeService.deActivate(UUID.randomUUID(), employeeId))
                .isInstanceOf(EmployeeNotFoundException.class);

        verify(employeeRepositoryPort, never()).save(any());
    }

    @Test
    void deActivate_shouldThrowEmployeeNotFoundException_whenEmployeeDoesNotExist() {
        when(employeeRepositoryPort.findById(employeeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deActivateEmployeeService.deActivate(veterinaryId, employeeId))
                .isInstanceOf(EmployeeNotFoundException.class);
    }

    @Test
    void deActivate_shouldThrowCannotModifySelfException_whenActorTargetsItsOwnRecord() {
        when(employeeRepositoryPort.findById(employeeId))
                .thenReturn(Optional.of(target.toBuilder().id(actingEmployeeId).build()));

        assertThatThrownBy(() -> deActivateEmployeeService.deActivate(veterinaryId, employeeId))
                .isInstanceOf(CannotModifySelfException.class);

        verify(employeeRepositoryPort, never()).save(any());
    }

    @Test
    void deActivate_shouldThrowCannotModifyOwnerException_whenTargetIsTheOwner() {
        when(employeeRepositoryPort.findById(employeeId))
                .thenReturn(Optional.of(target.toBuilder().role(EmployeeRole.OWNER).build()));

        assertThatThrownBy(() -> deActivateEmployeeService.deActivate(veterinaryId, employeeId))
                .isInstanceOf(CannotModifyOwnerException.class);
    }

    @Test
    void deActivate_shouldThrowCannotModifyPeerAdminException_whenAnAdminTargetsAnotherAdmin() {
        actingAs(EmployeeRole.ADMIN);
        when(employeeRepositoryPort.findById(employeeId))
                .thenReturn(Optional.of(target.toBuilder().role(EmployeeRole.ADMIN).build()));

        assertThatThrownBy(() -> deActivateEmployeeService.deActivate(veterinaryId, employeeId))
                .isInstanceOf(CannotModifyPeerAdminException.class);
    }

    @Test
    void deActivate_shouldPublishEmployeeDeactivatedToTheRemovedEmployee() {
        deActivateEmployeeService.deActivate(veterinaryId, employeeId);

        ArgumentCaptor<DomainEvent> captor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisherPort).publish(captor.capture());
        DomainEvent event = captor.getValue();
        assertThat(event.eventType()).isEqualTo(VeterinaryEventType.EMPLOYEE_DEACTIVATED);
        assertThat(event.userId()).isEqualTo(target.getUserId());
        assertThat(event.payload()).containsEntry("leftVoluntarily", false);
        assertThat(event.payload()).containsKey("removedAt");
    }

    @Test
    void deActivate_shouldNotTouchAnythingWhenAuthorizationFails() {
        for (RuntimeException rejection : new RuntimeException[]{
                new InsufficientPermissionException(Permission.STAFF_MANAGE),
                new NotClinicMemberException(),
                new ClinicSuspendedException()}) {
            // doThrow y no when(...).thenThrow(...): con when() la llamada dentro del propio when
            // ejecutaría el stub anterior y lanzaría antes de re-stubear.
            doThrow(rejection).when(authorizationService).require(veterinaryId, Permission.STAFF_MANAGE);

            assertThatThrownBy(() -> deActivateEmployeeService.deActivate(veterinaryId, employeeId))
                    .isInstanceOf(rejection.getClass());
        }

        verify(employeeRepositoryPort, never()).save(any());
        verify(eventPublisherPort, never()).publish(any());
    }
}

package com.MyAnimaLog.Veterinary.application.schedule.services;

import com.MyAnimaLog.Veterinary.application.schedule.dto.CreateScheduleBlockRequest;
import com.MyAnimaLog.Veterinary.application.schedule.dto.ListScheduleBlocksResponse;
import com.MyAnimaLog.Veterinary.application.schedule.dto.ScheduleBlockResponse;
import com.MyAnimaLog.Veterinary.application.schedule.ports.out.ScheduleBlockRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.dto.ClinicContext;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.schedule.exceptions.InvalidScheduleBlockException;
import com.MyAnimaLog.Veterinary.domain.schedule.exceptions.InvalidScheduleRangeException;
import com.MyAnimaLog.Veterinary.domain.schedule.exceptions.ScheduleBlockNotFoundException;
import com.MyAnimaLog.Veterinary.domain.schedule.model.ScheduleBlock;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.InsufficientPermissionException;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.domain.staff.model.VeterinaryEmployee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduleBlockServicesTest {

    private static final OffsetDateTime FROM = OffsetDateTime.of(2026, 3, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime TO = FROM.plusDays(30);

    @Mock
    private VeterinaryAuthorizationService authorizationService;
    @Mock
    private VeterinaryEmployeeRepositoryPort employeeRepositoryPort;
    @Mock
    private ScheduleBlockRepositoryPort scheduleBlockRepositoryPort;

    @InjectMocks
    private CreateScheduleBlockService createScheduleBlockService;
    @InjectMocks
    private DeleteScheduleBlockService deleteScheduleBlockService;
    @InjectMocks
    private ListScheduleBlocksService listScheduleBlocksService;

    private UUID veterinaryId;
    private UUID callerEmployeeId;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();
        callerEmployeeId = UUID.randomUUID();
    }

    private void callerIs(EmployeeRole role) {
        when(authorizationService.requireActiveMember(veterinaryId)).thenReturn(
                new ClinicContext(veterinaryId, callerEmployeeId, role, false, VeterinaryStatus.ACTIVE));
    }

    private void employeeOfThisClinic(UUID employeeId) {
        when(employeeRepositoryPort.findById(employeeId)).thenReturn(Optional.of(VeterinaryEmployee.builder()
                .id(employeeId).veterinaryId(veterinaryId).userId(UUID.randomUUID())
                .role(EmployeeRole.VETERINARIAN).active(true).build()));
    }

    private void echoSave() {
        when(scheduleBlockRepositoryPort.save(any(ScheduleBlock.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private CreateScheduleBlockRequest request(UUID employeeId, OffsetDateTime starts, OffsetDateTime ends) {
        return CreateScheduleBlockRequest.builder()
                .employeeId(employeeId).startsAt(starts).endsAt(ends).reason("Vacaciones").build();
    }

    @Test
    void create_shouldLetAnEmployeeBlockTheirOwnTime_withoutClinicConfigure() {
        callerIs(EmployeeRole.VETERINARIAN);
        employeeOfThisClinic(callerEmployeeId);
        echoSave();

        ScheduleBlockResponse response = createScheduleBlockService.create(veterinaryId,
                request(callerEmployeeId, FROM, FROM.plusHours(2)));

        assertThat(response.getEmployeeId()).isEqualTo(callerEmployeeId);
        ArgumentCaptor<ScheduleBlock> saved = ArgumentCaptor.forClass(ScheduleBlock.class);
        verify(scheduleBlockRepositoryPort).save(saved.capture());
        assertThat(saved.getValue().getVeterinaryId()).isEqualTo(veterinaryId);
    }

    @Test
    void create_shouldRejectAnEmployeeBlockingSomeoneElse() {
        callerIs(EmployeeRole.VETERINARIAN);

        assertThatThrownBy(() -> createScheduleBlockService.create(veterinaryId,
                request(UUID.randomUUID(), FROM, FROM.plusHours(2))))
                .isInstanceOf(InsufficientPermissionException.class);

        verify(scheduleBlockRepositoryPort, never()).save(any());
    }

    @Test
    void create_shouldRejectAWholeClinicBlock_withoutClinicConfigure() {
        callerIs(EmployeeRole.RECEPTIONIST);

        assertThatThrownBy(() -> createScheduleBlockService.create(veterinaryId,
                request(null, FROM, FROM.plusHours(2))))
                .isInstanceOf(InsufficientPermissionException.class);

        verify(scheduleBlockRepositoryPort, never()).save(any());
    }

    @Test
    void create_shouldAllowAWholeClinicBlock_withClinicConfigure() {
        callerIs(EmployeeRole.ADMIN);
        echoSave();

        ScheduleBlockResponse response = createScheduleBlockService.create(veterinaryId,
                request(null, FROM, FROM.plusDays(1)));

        assertThat(response.getEmployeeId()).isNull();
    }

    @Test
    void create_shouldAllowBlockingAnotherEmployee_withClinicConfigure() {
        UUID otherEmployeeId = UUID.randomUUID();
        callerIs(EmployeeRole.OWNER);
        employeeOfThisClinic(otherEmployeeId);
        echoSave();

        assertThat(createScheduleBlockService.create(veterinaryId,
                request(otherEmployeeId, FROM, FROM.plusHours(3))).getEmployeeId())
                .isEqualTo(otherEmployeeId);
    }

    @Test
    void create_shouldRejectEndsAtBeforeOrEqualStartsAt() {
        callerIs(EmployeeRole.ADMIN);

        assertThatThrownBy(() -> createScheduleBlockService.create(veterinaryId, request(null, FROM, FROM)))
                .isInstanceOf(InvalidScheduleBlockException.class);
        assertThatThrownBy(() -> createScheduleBlockService.create(veterinaryId,
                request(null, FROM, FROM.minusHours(1))))
                .isInstanceOf(InvalidScheduleBlockException.class);

        verify(scheduleBlockRepositoryPort, never()).save(any());
    }

    @Test
    void create_shouldRejectAReasonLongerThan200Characters() {
        callerIs(EmployeeRole.ADMIN);

        assertThatThrownBy(() -> createScheduleBlockService.create(veterinaryId,
                CreateScheduleBlockRequest.builder()
                        .startsAt(FROM).endsAt(FROM.plusHours(1)).reason("x".repeat(201)).build()))
                .isInstanceOf(InvalidScheduleBlockException.class);
    }

    @Test
    void delete_shouldLetTheOwningEmployeeDeleteTheirOwnBlock() {
        callerIs(EmployeeRole.VETERINARIAN);
        UUID blockId = UUID.randomUUID();
        when(scheduleBlockRepositoryPort.findByVeterinaryIdAndId(veterinaryId, blockId))
                .thenReturn(Optional.of(ScheduleBlock.builder()
                        .id(blockId).veterinaryId(veterinaryId).employeeId(callerEmployeeId)
                        .startsAt(FROM).endsAt(FROM.plusHours(1)).build()));

        deleteScheduleBlockService.delete(veterinaryId, blockId);

        verify(scheduleBlockRepositoryPort).deleteById(blockId);
    }

    @Test
    void delete_shouldRejectDeletingSomeoneElsesBlock_withoutClinicConfigure() {
        callerIs(EmployeeRole.VETERINARIAN);
        UUID blockId = UUID.randomUUID();
        when(scheduleBlockRepositoryPort.findByVeterinaryIdAndId(veterinaryId, blockId))
                .thenReturn(Optional.of(ScheduleBlock.builder()
                        .id(blockId).veterinaryId(veterinaryId).employeeId(UUID.randomUUID())
                        .startsAt(FROM).endsAt(FROM.plusHours(1)).build()));

        assertThatThrownBy(() -> deleteScheduleBlockService.delete(veterinaryId, blockId))
                .isInstanceOf(InsufficientPermissionException.class);

        verify(scheduleBlockRepositoryPort, never()).deleteById(any());
    }

    @Test
    void delete_shouldThrowNotFound_whenTheBlockBelongsToAnotherClinic() {
        callerIs(EmployeeRole.OWNER);
        UUID blockId = UUID.randomUUID();
        when(scheduleBlockRepositoryPort.findByVeterinaryIdAndId(veterinaryId, blockId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> deleteScheduleBlockService.delete(veterinaryId, blockId))
                .isInstanceOf(ScheduleBlockNotFoundException.class);

        verify(scheduleBlockRepositoryPort, never()).deleteById(any());
    }

    @Test
    void list_shouldRejectAnInvalidRange() {
        assertThatThrownBy(() -> listScheduleBlocksService.list(veterinaryId, null, TO))
                .isInstanceOf(InvalidScheduleRangeException.class);
        assertThatThrownBy(() -> listScheduleBlocksService.list(veterinaryId, FROM, null))
                .isInstanceOf(InvalidScheduleRangeException.class);
        assertThatThrownBy(() -> listScheduleBlocksService.list(veterinaryId, FROM, FROM.plusDays(93)))
                .isInstanceOf(InvalidScheduleRangeException.class);

        verify(scheduleBlockRepositoryPort, never()).findOverlapping(any(), any(), any());
    }

    @Test
    void list_shouldReturnOverlappingBlocks() {
        when(scheduleBlockRepositoryPort.findOverlapping(veterinaryId, FROM, TO)).thenReturn(List.of(
                ScheduleBlock.builder().id(UUID.randomUUID()).veterinaryId(veterinaryId)
                        .startsAt(FROM.minusDays(1)).endsAt(FROM.plusDays(1)).reason("Festivo").build()));

        ListScheduleBlocksResponse response = listScheduleBlocksService.list(veterinaryId, FROM, TO);

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getEmployeeId()).isNull();
        assertThat(response.getItems().get(0).getReason()).isEqualTo("Festivo");
    }
}

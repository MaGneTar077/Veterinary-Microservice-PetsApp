package com.MyAnimaLog.Veterinary.application.schedule.services;

import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.schedule.dto.GetScheduleRulesResponse;
import com.MyAnimaLog.Veterinary.application.schedule.ports.out.BusinessHoursRepositoryPort;
import com.MyAnimaLog.Veterinary.application.schedule.ports.out.EmployeeScheduleRepositoryPort;
import com.MyAnimaLog.Veterinary.application.schedule.ports.out.ScheduleBlockRepositoryPort;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort.EmployeeLicense;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.schedule.exceptions.InvalidScheduleRangeException;
import com.MyAnimaLog.Veterinary.domain.schedule.model.BusinessHours;
import com.MyAnimaLog.Veterinary.domain.schedule.model.EmployeeSchedule;
import com.MyAnimaLog.Veterinary.domain.schedule.model.ScheduleBlock;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VeterinaryNotFoundException;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.EmployeeNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
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
class GetScheduleRulesServiceTest {

    private static final OffsetDateTime FROM = OffsetDateTime.of(2026, 3, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime TO = FROM.plusDays(14);

    @Mock
    private VeterinaryRepositoryPort veterinaryRepositoryPort;
    @Mock
    private VeterinaryEmployeeRepositoryPort employeeRepositoryPort;
    @Mock
    private BusinessHoursRepositoryPort businessHoursRepositoryPort;
    @Mock
    private EmployeeScheduleRepositoryPort employeeScheduleRepositoryPort;
    @Mock
    private ScheduleBlockRepositoryPort scheduleBlockRepositoryPort;

    @InjectMocks
    private GetScheduleRulesService getScheduleRulesService;

    private UUID veterinaryId;
    private UUID employeeWithRows;
    private UUID employeeWithoutRows;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();
        employeeWithRows = UUID.randomUUID();
        employeeWithoutRows = UUID.randomUUID();
    }

    private void clinicExists() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(Veterinary.builder()
                .id(veterinaryId).name("Clínica").city("Bogotá").tenantId(UUID.randomUUID().toString())
                .status(VeterinaryStatus.ACTIVE).active(true).timezone("America/Bogota").build()));
    }

    private void activeEmployees(UUID... ids) {
        when(employeeRepositoryPort.findActiveWithLicense(veterinaryId)).thenReturn(
                java.util.Arrays.stream(ids)
                        .map(id -> new EmployeeLicense(id, EmployeeRole.VETERINARIAN, true))
                        .toList());
    }

    private void clinicHours(int... days) {
        when(businessHoursRepositoryPort.findByVeterinaryId(veterinaryId)).thenReturn(
                java.util.Arrays.stream(days)
                        .mapToObj(day -> BusinessHours.builder()
                                .id(UUID.randomUUID()).veterinaryId(veterinaryId).dayOfWeek(day)
                                .opensAt(LocalTime.of(8, 0)).closesAt(LocalTime.of(17, 0)).build())
                        .toList());
    }

    private EmployeeSchedule mondayOnly(UUID employeeId) {
        return EmployeeSchedule.builder()
                .id(UUID.randomUUID()).veterinaryId(veterinaryId).employeeId(employeeId)
                .dayOfWeek(1).startsAt(LocalTime.of(9, 0)).endsAt(LocalTime.of(13, 0)).build();
    }

    @Test
    void get_shouldReturnTheExactShapeCalendarExpects() {
        clinicExists();
        activeEmployees(employeeWithoutRows);
        clinicHours(1);
        when(employeeScheduleRepositoryPort.findByVeterinaryId(veterinaryId)).thenReturn(List.of());
        when(scheduleBlockRepositoryPort.findOverlapping(veterinaryId, FROM, TO)).thenReturn(List.of(
                ScheduleBlock.builder().id(UUID.randomUUID()).veterinaryId(veterinaryId)
                        .startsAt(FROM).endsAt(FROM.plusDays(1)).reason("Festivo").build()));

        GetScheduleRulesResponse response = getScheduleRulesService.get(veterinaryId, FROM, TO, null);

        assertThat(response.getTimezone()).isEqualTo("America/Bogota");
        assertThat(response.getBusinessHours()).hasSize(1);
        assertThat(response.getBusinessHours().get(0).getOpensAt()).isEqualTo("08:00");
        assertThat(response.getBusinessHours().get(0).getClosesAt()).isEqualTo("17:00");
        assertThat(response.getBlocks()).hasSize(1);
        assertThat(response.getBlocks().get(0).getEmployeeId()).isNull();
        assertThat(response.getBlocks().get(0).getReason()).isEqualTo("Festivo");
    }

    /** Sin filas propias, el empleado hereda el horario de la clínica. */
    @Test
    void get_shouldCopyClinicHours_forAnEmployeeWithoutRows() {
        clinicExists();
        activeEmployees(employeeWithoutRows);
        clinicHours(1, 2);
        when(employeeScheduleRepositoryPort.findByVeterinaryId(veterinaryId)).thenReturn(List.of());
        when(scheduleBlockRepositoryPort.findOverlapping(any(), any(), any())).thenReturn(List.of());

        GetScheduleRulesResponse response = getScheduleRulesService.get(veterinaryId, FROM, TO, null);

        assertThat(response.getEmployeeHours()).hasSize(2);
        assertThat(response.getEmployeeHours()).allMatch(h -> employeeWithoutRows.equals(h.getEmployeeId()));
        assertThat(response.getEmployeeHours()).extracting(GetScheduleRulesResponse.EmployeeHour::getDayOfWeek)
                .containsExactly(1, 2);
    }

    /** Con filas propias, esas filas son el horario completo: un día sin fila no se completa. */
    @Test
    void get_shouldReturnOnlyOwnRows_forAnEmployeeWithRows() {
        clinicExists();
        activeEmployees(employeeWithRows);
        clinicHours(1, 2);
        when(employeeScheduleRepositoryPort.findByVeterinaryId(veterinaryId))
                .thenReturn(List.of(mondayOnly(employeeWithRows)));
        when(scheduleBlockRepositoryPort.findOverlapping(any(), any(), any())).thenReturn(List.of());

        GetScheduleRulesResponse response = getScheduleRulesService.get(veterinaryId, FROM, TO, null);

        assertThat(response.getEmployeeHours()).hasSize(1);
        assertThat(response.getEmployeeHours().get(0).getDayOfWeek()).isEqualTo(1);
        assertThat(response.getEmployeeHours().get(0).getOpensAt()).isEqualTo("09:00");
        assertThat(response.getEmployeeHours()).noneMatch(h -> h.getDayOfWeek() == 2);
    }

    @Test
    void get_shouldReturnNoEmployeeHours_whenTheClinicHasNoBusinessHoursAndTheEmployeeHasNoRows() {
        clinicExists();
        activeEmployees(employeeWithoutRows);
        clinicHours();
        when(employeeScheduleRepositoryPort.findByVeterinaryId(veterinaryId)).thenReturn(List.of());
        when(scheduleBlockRepositoryPort.findOverlapping(any(), any(), any())).thenReturn(List.of());

        GetScheduleRulesResponse response = getScheduleRulesService.get(veterinaryId, FROM, TO, null);

        assertThat(response.getBusinessHours()).isEmpty();
        assertThat(response.getEmployeeHours()).isEmpty();
    }

    @Test
    void get_shouldApplyTheSameRule_whenFilteringByEmployeeId() {
        clinicExists();
        activeEmployees(employeeWithRows, employeeWithoutRows);
        clinicHours(1, 2);
        when(employeeScheduleRepositoryPort.findByVeterinaryId(veterinaryId))
                .thenReturn(List.of(mondayOnly(employeeWithRows)));
        when(scheduleBlockRepositoryPort.findOverlappingForEmployeeOrClinic(
                veterinaryId, employeeWithoutRows, FROM, TO)).thenReturn(List.of());

        GetScheduleRulesResponse response =
                getScheduleRulesService.get(veterinaryId, FROM, TO, employeeWithoutRows);

        assertThat(response.getEmployeeHours()).hasSize(2);
        assertThat(response.getEmployeeHours()).allMatch(h -> employeeWithoutRows.equals(h.getEmployeeId()));
    }

    @Test
    void get_shouldUseTheEmployeeOrClinicBlockQuery_whenFilteringByEmployeeId() {
        clinicExists();
        activeEmployees(employeeWithRows);
        clinicHours(1);
        when(employeeScheduleRepositoryPort.findByVeterinaryId(veterinaryId))
                .thenReturn(List.of(mondayOnly(employeeWithRows)));
        when(scheduleBlockRepositoryPort.findOverlappingForEmployeeOrClinic(
                veterinaryId, employeeWithRows, FROM, TO)).thenReturn(List.of());

        getScheduleRulesService.get(veterinaryId, FROM, TO, employeeWithRows);

        verify(scheduleBlockRepositoryPort).findOverlappingForEmployeeOrClinic(
                veterinaryId, employeeWithRows, FROM, TO);
        verify(scheduleBlockRepositoryPort, never()).findOverlapping(any(), any(), any());
    }

    @Test
    void get_shouldExcludeRemovedEmployees() {
        clinicExists();
        activeEmployees(employeeWithoutRows);
        clinicHours(1);
        // El empleado retirado tiene filas guardadas, pero no está en el roster activo.
        when(employeeScheduleRepositoryPort.findByVeterinaryId(veterinaryId))
                .thenReturn(List.of(mondayOnly(UUID.randomUUID())));
        when(scheduleBlockRepositoryPort.findOverlapping(any(), any(), any())).thenReturn(List.of());

        GetScheduleRulesResponse response = getScheduleRulesService.get(veterinaryId, FROM, TO, null);

        assertThat(response.getEmployeeHours()).allMatch(h -> employeeWithoutRows.equals(h.getEmployeeId()));
    }

    @Test
    void get_shouldThrowEmployeeNotFound_whenTheEmployeeIsNotActiveInThisClinic() {
        clinicExists();
        activeEmployees(employeeWithRows);

        assertThatThrownBy(() -> getScheduleRulesService.get(veterinaryId, FROM, TO, UUID.randomUUID()))
                .isInstanceOf(EmployeeNotFoundException.class);
    }

    @Test
    void get_shouldThrowVeterinaryNotFound_whenTheClinicDoesNotExist() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> getScheduleRulesService.get(veterinaryId, FROM, TO, null))
                .isInstanceOf(VeterinaryNotFoundException.class);
    }

    @Test
    void get_shouldRejectAnInvalidRange_beforeTouchingTheDatabase() {
        assertThatThrownBy(() -> getScheduleRulesService.get(veterinaryId, null, TO, null))
                .isInstanceOf(InvalidScheduleRangeException.class);
        assertThatThrownBy(() -> getScheduleRulesService.get(veterinaryId, FROM, null, null))
                .isInstanceOf(InvalidScheduleRangeException.class);
        assertThatThrownBy(() -> getScheduleRulesService.get(veterinaryId, FROM, FROM, null))
                .isInstanceOf(InvalidScheduleRangeException.class);
        assertThatThrownBy(() -> getScheduleRulesService.get(veterinaryId, FROM, FROM.plusDays(93), null))
                .isInstanceOf(InvalidScheduleRangeException.class);

        verify(veterinaryRepositoryPort, never()).findById(any());
    }
}

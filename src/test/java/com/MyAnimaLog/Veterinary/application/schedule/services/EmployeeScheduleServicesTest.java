package com.MyAnimaLog.Veterinary.application.schedule.services;

import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.schedule.dto.EmployeeScheduleResponse;
import com.MyAnimaLog.Veterinary.application.schedule.dto.ScheduleEntryRequest;
import com.MyAnimaLog.Veterinary.application.schedule.ports.out.EmployeeScheduleRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.dto.ClinicContext;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.schedule.model.EmployeeSchedule;
import com.MyAnimaLog.Veterinary.domain.security.Permission;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.InsufficientPermissionException;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.EmployeeNotFoundException;
import com.MyAnimaLog.Veterinary.domain.staff.model.VeterinaryEmployee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeScheduleServicesTest {

    @Mock
    private VeterinaryAuthorizationService authorizationService;
    @Mock
    private VeterinaryRepositoryPort veterinaryRepositoryPort;
    @Mock
    private VeterinaryEmployeeRepositoryPort employeeRepositoryPort;
    @Mock
    private EmployeeScheduleRepositoryPort employeeScheduleRepositoryPort;

    @InjectMocks
    private GetEmployeeScheduleService getEmployeeScheduleService;
    @InjectMocks
    private ReplaceEmployeeScheduleService replaceEmployeeScheduleService;

    private UUID veterinaryId;
    private UUID employeeId;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();
        employeeId = UUID.randomUUID();
    }

    private void clinicExists() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(Veterinary.builder()
                .id(veterinaryId).name("Clínica").city("Bogotá").tenantId(UUID.randomUUID().toString())
                .status(VeterinaryStatus.ACTIVE).active(true).timezone("America/Bogota").build()));
    }

    private void employeeExists(boolean active, UUID ofClinic) {
        when(employeeRepositoryPort.findById(employeeId)).thenReturn(Optional.of(VeterinaryEmployee.builder()
                .id(employeeId).veterinaryId(ofClinic).userId(UUID.randomUUID())
                .role(EmployeeRole.VETERINARIAN).active(active).build()));
    }

    @Test
    void get_shouldReturnEmptyItemsAndUsesClinicHours_whenTheEmployeeHasNoRows() {
        employeeExists(true, veterinaryId);
        clinicExists();
        when(employeeScheduleRepositoryPort.findByVeterinaryIdAndEmployeeId(veterinaryId, employeeId))
                .thenReturn(List.of());

        EmployeeScheduleResponse response = getEmployeeScheduleService.get(veterinaryId, employeeId);

        assertThat(response.getItems()).isEmpty();
        assertThat(response.isUsesClinicHours()).isTrue();
    }

    @Test
    void get_shouldReturnTheEmployeeRowsAsIs() {
        employeeExists(true, veterinaryId);
        clinicExists();
        when(employeeScheduleRepositoryPort.findByVeterinaryIdAndEmployeeId(veterinaryId, employeeId))
                .thenReturn(List.of(EmployeeSchedule.builder()
                        .id(UUID.randomUUID()).veterinaryId(veterinaryId).employeeId(employeeId)
                        .dayOfWeek(1).startsAt(LocalTime.of(9, 0)).endsAt(LocalTime.of(13, 0)).build()));

        EmployeeScheduleResponse response = getEmployeeScheduleService.get(veterinaryId, employeeId);

        assertThat(response.isUsesClinicHours()).isFalse();
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getOpensAt()).isEqualTo("09:00");
        assertThat(response.getItems().get(0).getClosesAt()).isEqualTo("13:00");
    }

    @Test
    void get_shouldThrowEmployeeNotFound_whenTheEmployeeBelongsToAnotherClinic() {
        employeeExists(true, UUID.randomUUID());

        assertThatThrownBy(() -> getEmployeeScheduleService.get(veterinaryId, employeeId))
                .isInstanceOf(EmployeeNotFoundException.class);
    }

    @Test
    void get_shouldThrowEmployeeNotFound_whenTheEmployeeWasRemoved() {
        employeeExists(false, veterinaryId);

        assertThatThrownBy(() -> getEmployeeScheduleService.get(veterinaryId, employeeId))
                .isInstanceOf(EmployeeNotFoundException.class);
    }

    @Test
    void replace_shouldDeleteAndInsertTheEmployeeRows() {
        employeeExists(true, veterinaryId);
        clinicExists();

        EmployeeScheduleResponse response = replaceEmployeeScheduleService.replace(veterinaryId, employeeId,
                List.of(ScheduleEntryRequest.builder().dayOfWeek(1).opensAt("09:00").closesAt("13:00").build()));

        verify(authorizationService).require(veterinaryId, Permission.CLINIC_CONFIGURE);
        verify(employeeScheduleRepositoryPort).deleteByVeterinaryIdAndEmployeeId(veterinaryId, employeeId);
        verify(employeeScheduleRepositoryPort).saveAll(anyList());
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.isUsesClinicHours()).isFalse();
    }

    @Test
    void replace_shouldAcceptAnEmptyList_returningTheEmployeeToClinicHours() {
        employeeExists(true, veterinaryId);
        clinicExists();

        EmployeeScheduleResponse response =
                replaceEmployeeScheduleService.replace(veterinaryId, employeeId, List.of());

        verify(employeeScheduleRepositoryPort).deleteByVeterinaryIdAndEmployeeId(veterinaryId, employeeId);
        verify(employeeScheduleRepositoryPort).saveAll(List.of());
        assertThat(response.isUsesClinicHours()).isTrue();
    }

    /** Calendar hace la intersección: acá no se exige caer dentro del horario de la clínica. */
    @Test
    void replace_shouldAllowHoursOutsideTheClinicHours() {
        employeeExists(true, veterinaryId);
        clinicExists();

        EmployeeScheduleResponse response = replaceEmployeeScheduleService.replace(veterinaryId, employeeId,
                List.of(ScheduleEntryRequest.builder().dayOfWeek(7).opensAt("03:00").closesAt("05:00").build()));

        assertThat(response.getItems()).hasSize(1);
        verify(employeeScheduleRepositoryPort).saveAll(anyList());
    }

    @Test
    void replace_shouldPropagateInsufficientPermission_whenCallerLacksClinicConfigure() {
        doThrow(new InsufficientPermissionException(Permission.CLINIC_CONFIGURE))
                .when(authorizationService).require(veterinaryId, Permission.CLINIC_CONFIGURE);

        assertThatThrownBy(() -> replaceEmployeeScheduleService.replace(veterinaryId, employeeId, List.of()))
                .isInstanceOf(InsufficientPermissionException.class);

        verify(employeeScheduleRepositoryPort, never()).deleteByVeterinaryIdAndEmployeeId(any(), any());
    }

    @Test
    void replace_shouldThrowEmployeeNotFound_whenTheEmployeeBelongsToAnotherClinic() {
        employeeExists(true, UUID.randomUUID());

        assertThatThrownBy(() -> replaceEmployeeScheduleService.replace(veterinaryId, employeeId, List.of()))
                .isInstanceOf(EmployeeNotFoundException.class);

        verify(employeeScheduleRepositoryPort, never()).deleteByVeterinaryIdAndEmployeeId(any(), any());
    }

    /** Leer el horario no exige permiso, solo ser miembro activo (ASSISTANT/RECEPTIONIST incluidos). */
    @Test
    void get_shouldOnlyRequireActiveMembership() {
        employeeExists(true, veterinaryId);
        clinicExists();
        when(employeeScheduleRepositoryPort.findByVeterinaryIdAndEmployeeId(veterinaryId, employeeId))
                .thenReturn(List.of());
        when(authorizationService.requireActiveMember(veterinaryId)).thenReturn(
                new ClinicContext(veterinaryId, UUID.randomUUID(), EmployeeRole.RECEPTIONIST, false,
                        VeterinaryStatus.ACTIVE));

        getEmployeeScheduleService.get(veterinaryId, employeeId);

        verify(authorizationService).requireActiveMember(veterinaryId);
        verify(authorizationService, never()).require(any(), any());
    }
}

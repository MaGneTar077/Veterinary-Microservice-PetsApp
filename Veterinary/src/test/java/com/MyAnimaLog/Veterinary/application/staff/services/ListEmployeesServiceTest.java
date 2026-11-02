package com.MyAnimaLog.Veterinary.application.staff.services;

import com.MyAnimaLog.Veterinary.application.shared.dto.ClinicContext;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.UserServicePort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.application.staff.dto.ListEmployeesResponse;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.ClinicSuspendedException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.NotClinicMemberException;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ListEmployeesServiceTest {

    @Mock
    private VeterinaryEmployeeRepositoryPort employeeRepositoryPort;

    @Mock
    private VeterinaryAuthorizationService authorizationService;

    @Mock
    private UserServicePort userServicePort;

    @InjectMocks
    private ListEmployeesService listEmployeesService;

    private UUID veterinaryId;
    private UUID ownerEmployeeId;
    private UUID ownerUserId;
    private VeterinaryEmployee owner;
    private VeterinaryEmployee removedAssistant;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();
        ownerEmployeeId = UUID.randomUUID();
        ownerUserId = UUID.randomUUID();

        owner = VeterinaryEmployee.builder()
                .id(ownerEmployeeId)
                .veterinaryId(veterinaryId)
                .userId(ownerUserId)
                .role(EmployeeRole.OWNER)
                .active(true)
                .createdAt(LocalDateTime.now().minusMonths(2))
                .build();

        removedAssistant = VeterinaryEmployee.builder()
                .id(UUID.randomUUID())
                .veterinaryId(veterinaryId)
                .userId(UUID.randomUUID())
                .role(EmployeeRole.ASSISTANT)
                .active(false)
                .createdAt(LocalDateTime.now().minusMonths(1))
                .removedAt(LocalDateTime.now().minusDays(2))
                .build();

        when(authorizationService.requireActiveMember(veterinaryId)).thenReturn(
                new ClinicContext(veterinaryId, ownerEmployeeId, EmployeeRole.OWNER, false, VeterinaryStatus.ACTIVE));
        when(employeeRepositoryPort.findByVeterinaryId(veterinaryId))
                .thenReturn(List.of(owner, removedAssistant));
        when(userServicePort.getUsers(anyList())).thenReturn(Map.of(
                ownerUserId, new UserServicePort.UserInfo(ownerUserId, "owner@clinica.com", true, "Ana Dueña")));
    }

    @Test
    void list_shouldReturnOnlyActiveEmployeesByDefault() {
        ListEmployeesResponse response = listEmployeesService.list(veterinaryId, false);

        assertThat(response.getEmployees()).hasSize(1);
        assertThat(response.getEmployees().get(0).getEmployeeId()).isEqualTo(ownerEmployeeId);
        assertThat(response.getEmployees().get(0).getRole()).isEqualTo(EmployeeRole.OWNER);
    }

    @Test
    void list_shouldIncludeRemovedEmployeesWithTheirRemovalDate_whenAsked() {
        ListEmployeesResponse response = listEmployeesService.list(veterinaryId, true);

        assertThat(response.getEmployees()).hasSize(2);
        ListEmployeesResponse.Employee removed = response.getEmployees().stream()
                .filter(employee -> !employee.getActive())
                .findFirst()
                .orElseThrow();
        assertThat(removed.getRemovedAt()).isEqualTo(removedAssistant.getRemovedAt());
    }

    @Test
    void list_shouldEnrichNameAndEmailFromTheUserService() {
        ListEmployeesResponse.Employee ownerRow = listEmployeesService.list(veterinaryId, false)
                .getEmployees().get(0);

        assertThat(ownerRow.getName()).isEqualTo("Ana Dueña");
        assertThat(ownerRow.getEmail()).isEqualTo("owner@clinica.com");
    }

    @Test
    void list_shouldStillAnswer_whenTheUserServiceCannotResolveSomeone() {
        when(userServicePort.getUsers(anyList())).thenReturn(Map.of());

        ListEmployeesResponse.Employee ownerRow = listEmployeesService.list(veterinaryId, false)
                .getEmployees().get(0);

        assertThat(ownerRow.getName()).isNull();
        assertThat(ownerRow.getEmail()).isNull();
        assertThat(ownerRow.getRole()).isEqualTo(EmployeeRole.OWNER);
    }

    @Test
    void list_shouldResolveUsersInASingleBatchCall() {
        listEmployeesService.list(veterinaryId, true);

        ArgumentCaptor<List<UUID>> captor = ArgumentCaptor.forClass(List.class);
        verify(userServicePort).getUsers(captor.capture());
        assertThat(captor.getValue()).containsExactlyInAnyOrder(ownerUserId, removedAssistant.getUserId());
    }

    @Test
    void list_shouldFlagTheCallerOwnRow() {
        ListEmployeesResponse response = listEmployeesService.list(veterinaryId, true);

        assertThat(response.getEmployees().stream().filter(ListEmployeesResponse.Employee::isSelf))
                .singleElement()
                .extracting(ListEmployeesResponse.Employee::getEmployeeId)
                .isEqualTo(ownerEmployeeId);
    }

    @Test
    void list_shouldRequireBeingAnActiveMemberOfTheClinic() {
        listEmployeesService.list(veterinaryId, false);

        verify(authorizationService).requireActiveMember(veterinaryId);
    }

    @Test
    void list_shouldPropagateAuthorizationFailures() {
        doThrow(new NotClinicMemberException()).when(authorizationService).requireActiveMember(veterinaryId);
        assertThatThrownBy(() -> listEmployeesService.list(veterinaryId, false))
                .isInstanceOf(NotClinicMemberException.class);

        doThrow(new ClinicSuspendedException()).when(authorizationService).requireActiveMember(veterinaryId);
        assertThatThrownBy(() -> listEmployeesService.list(veterinaryId, false))
                .isInstanceOf(ClinicSuspendedException.class);
    }
}

package com.MyAnimaLog.Veterinary.application.catalog.services;

import com.MyAnimaLog.Veterinary.application.catalog.dto.InternalClinicServiceResponse;
import com.MyAnimaLog.Veterinary.application.catalog.ports.out.ClinicServiceRepositoryPort;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort.EmployeeLicense;
import com.MyAnimaLog.Veterinary.domain.catalog.enums.ServiceCategory;
import com.MyAnimaLog.Veterinary.domain.catalog.exceptions.InvalidClinicServiceException;
import com.MyAnimaLog.Veterinary.domain.catalog.model.ClinicService;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetInternalClinicServicesServiceTest {

    @Mock
    private ClinicServiceRepositoryPort repositoryPort;
    @Mock
    private VeterinaryEmployeeRepositoryPort employeeRepositoryPort;

    @InjectMocks
    private GetInternalClinicServicesService getInternalClinicServicesService;

    private UUID veterinaryId;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();
    }

    private ClinicService service(String name, boolean requiresLicense) {
        return service(name, requiresLicense, true, true);
    }

    private ClinicService service(String name, boolean requiresLicense, boolean active, boolean bookableOnline) {
        return ClinicService.builder()
                .id(UUID.randomUUID())
                .veterinaryId(veterinaryId)
                .name(name)
                .category(ServiceCategory.CONSULTATION)
                .durationMinutes(30)
                .price(BigDecimal.valueOf(40000))
                .bookableOnline(bookableOnline)
                .active(active)
                .requiresLicense(requiresLicense)
                .allowedRoles(List.of(EmployeeRole.VETERINARIAN))
                .build();
    }

    /**
     * El roster de empleados se consulta <strong>una sola vez por petición</strong>, no una vez por
     * servicio: con 3 servicios sigue siendo una consulta. Si alguien mueve la llamada dentro del
     * {@code map(...)}, este test falla.
     */
    @Test
    void list_shouldQueryEmployeesOncePerRequest_notPerService() {
        when(repositoryPort.findByVeterinaryId(veterinaryId)).thenReturn(List.of(
                service("Consulta", false), service("Cirugía", true), service("Vacunación", false)));
        when(employeeRepositoryPort.findActiveWithLicense(veterinaryId)).thenReturn(List.of(
                new EmployeeLicense(UUID.randomUUID(), EmployeeRole.VETERINARIAN, true)));

        List<InternalClinicServiceResponse> response =
                getInternalClinicServicesService.list(veterinaryId, false, null);

        assertThat(response).hasSize(3);
        verify(employeeRepositoryPort, times(1)).findActiveWithLicense(veterinaryId);
    }

    @Test
    void list_shouldReturnEveryService_whenActiveOnlyIsFalse() {
        when(repositoryPort.findByVeterinaryId(veterinaryId)).thenReturn(List.of(service("Consulta", false)));
        when(employeeRepositoryPort.findActiveWithLicense(veterinaryId)).thenReturn(List.of());

        getInternalClinicServicesService.list(veterinaryId, false, null);

        verify(repositoryPort).findByVeterinaryId(veterinaryId);
        verify(repositoryPort, never()).findByVeterinaryIdAndActiveTrue(veterinaryId);
    }

    @Test
    void list_shouldOnlyReturnActiveServices_whenActiveOnlyIsTrue() {
        when(repositoryPort.findByVeterinaryIdAndActiveTrue(veterinaryId))
                .thenReturn(List.of(service("Consulta", false)));
        when(employeeRepositoryPort.findActiveWithLicense(veterinaryId)).thenReturn(List.of());

        getInternalClinicServicesService.list(veterinaryId, true, null);

        verify(repositoryPort).findByVeterinaryIdAndActiveTrue(veterinaryId);
        verify(repositoryPort, never()).findByVeterinaryId(veterinaryId);
    }

    @Test
    void list_shouldReturnEmptyEmployeeIds_whenNobodyIsEligible() {
        when(repositoryPort.findByVeterinaryId(veterinaryId)).thenReturn(List.of(service("Cirugía", true)));
        when(employeeRepositoryPort.findActiveWithLicense(veterinaryId)).thenReturn(List.of(
                new EmployeeLicense(UUID.randomUUID(), EmployeeRole.VETERINARIAN, false)));

        List<InternalClinicServiceResponse> response =
                getInternalClinicServicesService.list(veterinaryId, false, null);

        assertThat(response.get(0).getEmployeeIds()).isEmpty();
    }

    // --- bloque b2: filtro bookableOnline ---

    @Test
    void list_shouldNotFilterByBookableOnline_whenTheParameterIsAbsent() {
        when(repositoryPort.findByVeterinaryId(veterinaryId)).thenReturn(List.of(
                service("Consulta", false, true, true),
                service("A domicilio", false, true, false)));
        when(employeeRepositoryPort.findActiveWithLicense(veterinaryId)).thenReturn(List.of());

        List<InternalClinicServiceResponse> response =
                getInternalClinicServicesService.list(veterinaryId, false, null);

        assertThat(response).hasSize(2);
    }

    @Test
    void list_shouldOnlyReturnBookableOnline_whenFilterIsTrue() {
        when(repositoryPort.findByVeterinaryId(veterinaryId)).thenReturn(List.of(
                service("Consulta", false, true, true),
                service("A domicilio", false, true, false)));
        when(employeeRepositoryPort.findActiveWithLicense(veterinaryId)).thenReturn(List.of());

        List<InternalClinicServiceResponse> response =
                getInternalClinicServicesService.list(veterinaryId, false, "true");

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getName()).isEqualTo("Consulta");
    }

    @Test
    void list_shouldOnlyReturnNonBookableOnline_whenFilterIsFalse() {
        when(repositoryPort.findByVeterinaryId(veterinaryId)).thenReturn(List.of(
                service("Consulta", false, true, true),
                service("A domicilio", false, true, false)));
        when(employeeRepositoryPort.findActiveWithLicense(veterinaryId)).thenReturn(List.of());

        List<InternalClinicServiceResponse> response =
                getInternalClinicServicesService.list(veterinaryId, false, "false");

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getName()).isEqualTo("A domicilio");
    }

    /** activeOnly y bookableOnline se combinan con AND: un servicio inactivo y no reservable solo
     *  aparece cuando ningún filtro lo excluye. */
    @Test
    void list_shouldCombineActiveOnlyAndBookableOnlineWithAnd() {
        when(repositoryPort.findByVeterinaryIdAndActiveTrue(veterinaryId)).thenReturn(List.of(
                service("Consulta", false, true, true)));
        when(employeeRepositoryPort.findActiveWithLicense(veterinaryId)).thenReturn(List.of());

        List<InternalClinicServiceResponse> response =
                getInternalClinicServicesService.list(veterinaryId, true, "false");

        assertThat(response).isEmpty();
    }

    @Test
    void list_shouldOnlyShowAnInactiveNonBookableService_whenNoFilterExcludesIt() {
        when(repositoryPort.findByVeterinaryId(veterinaryId)).thenReturn(List.of(
                service("Vieja", false, false, false)));
        when(employeeRepositoryPort.findActiveWithLicense(veterinaryId)).thenReturn(List.of());

        List<InternalClinicServiceResponse> withoutFilters =
                getInternalClinicServicesService.list(veterinaryId, false, null);
        assertThat(withoutFilters).hasSize(1);

        List<InternalClinicServiceResponse> withBookableTrue =
                getInternalClinicServicesService.list(veterinaryId, false, "true");
        assertThat(withBookableTrue).isEmpty();
    }

    @Test
    void list_shouldRejectAnInvalidBookableOnlineValue() {
        assertThatThrownBy(() -> getInternalClinicServicesService.list(veterinaryId, false, "yes"))
                .isInstanceOf(InvalidClinicServiceException.class);

        verify(repositoryPort, never()).findByVeterinaryId(any());
        verify(repositoryPort, never()).findByVeterinaryIdAndActiveTrue(any());
    }
}

package com.MyAnimaLog.Veterinary.application.catalog.services;

import com.MyAnimaLog.Veterinary.application.catalog.dto.InternalClinicServiceResponse;
import com.MyAnimaLog.Veterinary.application.catalog.ports.out.ClinicServiceRepositoryPort;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort.EmployeeLicense;
import com.MyAnimaLog.Veterinary.domain.catalog.enums.ServiceCategory;
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
        return ClinicService.builder()
                .id(UUID.randomUUID())
                .veterinaryId(veterinaryId)
                .name(name)
                .category(ServiceCategory.CONSULTATION)
                .durationMinutes(30)
                .price(BigDecimal.valueOf(40000))
                .bookableOnline(true)
                .active(true)
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

        List<InternalClinicServiceResponse> response = getInternalClinicServicesService.list(veterinaryId, false);

        assertThat(response).hasSize(3);
        verify(employeeRepositoryPort, times(1)).findActiveWithLicense(veterinaryId);
    }

    @Test
    void list_shouldReturnEveryService_whenActiveOnlyIsFalse() {
        when(repositoryPort.findByVeterinaryId(veterinaryId)).thenReturn(List.of(service("Consulta", false)));
        when(employeeRepositoryPort.findActiveWithLicense(veterinaryId)).thenReturn(List.of());

        getInternalClinicServicesService.list(veterinaryId, false);

        verify(repositoryPort).findByVeterinaryId(veterinaryId);
        verify(repositoryPort, never()).findByVeterinaryIdAndActiveTrue(veterinaryId);
    }

    @Test
    void list_shouldOnlyReturnActiveServices_whenActiveOnlyIsTrue() {
        when(repositoryPort.findByVeterinaryIdAndActiveTrue(veterinaryId))
                .thenReturn(List.of(service("Consulta", false)));
        when(employeeRepositoryPort.findActiveWithLicense(veterinaryId)).thenReturn(List.of());

        getInternalClinicServicesService.list(veterinaryId, true);

        verify(repositoryPort).findByVeterinaryIdAndActiveTrue(veterinaryId);
        verify(repositoryPort, never()).findByVeterinaryId(veterinaryId);
    }

    @Test
    void list_shouldReturnEmptyEmployeeIds_whenNobodyIsEligible() {
        when(repositoryPort.findByVeterinaryId(veterinaryId)).thenReturn(List.of(service("Cirugía", true)));
        when(employeeRepositoryPort.findActiveWithLicense(veterinaryId)).thenReturn(List.of(
                new EmployeeLicense(UUID.randomUUID(), EmployeeRole.VETERINARIAN, false)));

        List<InternalClinicServiceResponse> response = getInternalClinicServicesService.list(veterinaryId, false);

        assertThat(response.get(0).getEmployeeIds()).isEmpty();
    }
}

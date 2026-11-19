package com.MyAnimaLog.Veterinary.application.catalog.services;

import com.MyAnimaLog.Veterinary.application.catalog.dto.InternalClinicServiceResponse;
import com.MyAnimaLog.Veterinary.application.catalog.ports.out.ClinicServiceRepositoryPort;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort.EmployeeLicense;
import com.MyAnimaLog.Veterinary.domain.catalog.enums.ServiceCategory;
import com.MyAnimaLog.Veterinary.domain.catalog.exceptions.ClinicServiceNotFoundException;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetInternalClinicServiceServiceTest {

    @Mock
    private ClinicServiceRepositoryPort repositoryPort;
    @Mock
    private VeterinaryEmployeeRepositoryPort employeeRepositoryPort;

    @InjectMocks
    private GetInternalClinicServiceService getInternalClinicServiceService;

    private UUID veterinaryId;
    private UUID serviceId;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();
        serviceId = UUID.randomUUID();
    }

    private ClinicService service(boolean active) {
        return ClinicService.builder()
                .id(serviceId)
                .veterinaryId(veterinaryId)
                .name("Consulta general")
                .category(ServiceCategory.CONSULTATION)
                .durationMinutes(30)
                .price(BigDecimal.valueOf(40000))
                .bookableOnline(true)
                .active(active)
                .requiresLicense(false)
                .allowedRoles(ClinicServiceValidation.DEFAULT_ALLOWED_ROLES)
                .build();
    }

    /** Calendar necesita el detalle de un servicio ya desactivado: hay citas que lo referencian. */
    @Test
    void get_shouldReturnInactiveServices() {
        UUID employeeId = UUID.randomUUID();
        when(repositoryPort.findByVeterinaryIdAndId(veterinaryId, serviceId))
                .thenReturn(Optional.of(service(false)));
        when(employeeRepositoryPort.findActiveWithLicense(veterinaryId))
                .thenReturn(List.of(new EmployeeLicense(employeeId, EmployeeRole.OWNER, false)));

        InternalClinicServiceResponse response = getInternalClinicServiceService.get(veterinaryId, serviceId);

        assertThat(response.getActive()).isFalse();
        assertThat(response.getId()).isEqualTo(serviceId);
        assertThat(response.getEmployeeIds()).containsExactly(employeeId);
    }

    @Test
    void get_shouldThrowNotFound_whenTheServiceBelongsToAnotherClinic() {
        when(repositoryPort.findByVeterinaryIdAndId(veterinaryId, serviceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> getInternalClinicServiceService.get(veterinaryId, serviceId))
                .isInstanceOf(ClinicServiceNotFoundException.class);

        verify(employeeRepositoryPort, never()).findActiveWithLicense(any());
    }
}

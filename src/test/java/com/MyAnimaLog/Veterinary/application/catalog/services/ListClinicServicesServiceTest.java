package com.MyAnimaLog.Veterinary.application.catalog.services;

import com.MyAnimaLog.Veterinary.application.catalog.dto.ListClinicServicesResponse;
import com.MyAnimaLog.Veterinary.application.catalog.ports.out.ClinicServiceRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.dto.ClinicContext;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.domain.catalog.enums.ServiceCategory;
import com.MyAnimaLog.Veterinary.domain.catalog.model.ClinicService;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * El catálogo lo puede listar cualquier miembro activo (ASSISTANT/RECEPTIONIST incluidos): usa
 * {@code requireActiveMember}, nunca {@code require(..., permiso)} — a diferencia de
 * crear/editar/desactivar, que exigen {@code CLINIC_CONFIGURE}.
 */
@ExtendWith(MockitoExtension.class)
class ListClinicServicesServiceTest {

    @Mock
    private VeterinaryAuthorizationService authorizationService;
    @Mock
    private ClinicServiceRepositoryPort repositoryPort;

    @InjectMocks
    private ListClinicServicesService listClinicServicesService;

    private UUID veterinaryId;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();
    }

    @Test
    void list_shouldOnlyRequireActiveMembership_notAPermission() {
        when(authorizationService.requireActiveMember(veterinaryId)).thenReturn(
                new ClinicContext(veterinaryId, UUID.randomUUID(), EmployeeRole.RECEPTIONIST, false, VeterinaryStatus.ACTIVE));
        when(repositoryPort.findByVeterinaryId(veterinaryId)).thenReturn(List.of());

        listClinicServicesService.list(veterinaryId);

        verify(authorizationService).requireActiveMember(veterinaryId);
        verify(authorizationService, never()).require(any(), any());
    }

    @Test
    void list_shouldReturnEveryService_activeAndInactive() {
        when(authorizationService.requireActiveMember(veterinaryId)).thenReturn(
                new ClinicContext(veterinaryId, UUID.randomUUID(), EmployeeRole.ASSISTANT, false, VeterinaryStatus.ACTIVE));
        when(repositoryPort.findByVeterinaryId(veterinaryId)).thenReturn(List.of(
                service("Consulta", true),
                service("Vacunación vieja", false)));

        ListClinicServicesResponse response = listClinicServicesService.list(veterinaryId);

        assertThat(response.getItems()).hasSize(2);
        assertThat(response.getItems()).extracting(ListClinicServicesResponse.Item::getActive)
                .containsExactlyInAnyOrder(true, false);
    }

    private ClinicService service(String name, boolean active) {
        return ClinicService.builder()
                .id(UUID.randomUUID())
                .veterinaryId(veterinaryId)
                .name(name)
                .category(ServiceCategory.CONSULTATION)
                .durationMinutes(30)
                .price(BigDecimal.valueOf(40000))
                .bookableOnline(true)
                .active(active)
                .requiresLicense(false)
                .allowedRoles(ClinicServiceValidation.DEFAULT_ALLOWED_ROLES)
                .build();
    }
}

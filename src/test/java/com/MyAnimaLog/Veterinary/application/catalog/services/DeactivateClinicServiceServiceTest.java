package com.MyAnimaLog.Veterinary.application.catalog.services;

import com.MyAnimaLog.Veterinary.application.catalog.dto.ClinicServiceResponse;
import com.MyAnimaLog.Veterinary.application.catalog.ports.out.ClinicServiceRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.domain.catalog.enums.ServiceCategory;
import com.MyAnimaLog.Veterinary.domain.catalog.exceptions.ClinicServiceNotFoundException;
import com.MyAnimaLog.Veterinary.domain.catalog.model.ClinicService;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.InsufficientPermissionException;
import com.MyAnimaLog.Veterinary.domain.security.Permission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeactivateClinicServiceServiceTest {

    @Mock
    private VeterinaryAuthorizationService authorizationService;
    @Mock
    private ClinicServiceRepositoryPort repositoryPort;

    @InjectMocks
    private DeactivateClinicServiceService deactivateClinicServiceService;

    private UUID veterinaryId;
    private UUID serviceId;
    private ClinicService existing;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();
        serviceId = UUID.randomUUID();
        existing = ClinicService.builder()
                .id(serviceId)
                .veterinaryId(veterinaryId)
                .name("Consulta general")
                .category(ServiceCategory.CONSULTATION)
                .durationMinutes(30)
                .price(BigDecimal.valueOf(40000))
                .bookableOnline(true)
                .active(true)
                .requiresLicense(false)
                .allowedRoles(ClinicServiceValidation.DEFAULT_ALLOWED_ROLES)
                .build();
    }

    @Test
    void deactivate_shouldSetActiveFalse_neverDeleting() {
        when(repositoryPort.findByVeterinaryIdAndId(veterinaryId, serviceId)).thenReturn(Optional.of(existing));
        when(repositoryPort.save(any(ClinicService.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ClinicServiceResponse response = deactivateClinicServiceService.deactivate(veterinaryId, serviceId);

        verify(authorizationService).require(veterinaryId, Permission.CLINIC_CONFIGURE);
        ArgumentCaptor<ClinicService> saved = ArgumentCaptor.forClass(ClinicService.class);
        verify(repositoryPort).save(saved.capture());
        assertThat(saved.getValue().getActive()).isFalse();
        assertThat(saved.getValue().getId()).isEqualTo(serviceId);
        assertThat(response.getActive()).isFalse();
    }

    @Test
    void deactivate_shouldThrowNotFound_whenServiceBelongsToAnotherClinic() {
        when(repositoryPort.findByVeterinaryIdAndId(veterinaryId, serviceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deactivateClinicServiceService.deactivate(veterinaryId, serviceId))
                .isInstanceOf(ClinicServiceNotFoundException.class);

        verify(repositoryPort, never()).save(any());
    }

    @Test
    void deactivate_shouldPropagateInsufficientPermission_whenCallerLacksClinicConfigure() {
        doThrow(new InsufficientPermissionException(Permission.CLINIC_CONFIGURE))
                .when(authorizationService).require(veterinaryId, Permission.CLINIC_CONFIGURE);

        assertThatThrownBy(() -> deactivateClinicServiceService.deactivate(veterinaryId, serviceId))
                .isInstanceOf(InsufficientPermissionException.class);
    }
}

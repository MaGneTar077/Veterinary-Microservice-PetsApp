package com.MyAnimaLog.Veterinary.application.catalog.services;

import com.MyAnimaLog.Veterinary.application.catalog.dto.ClinicServiceResponse;
import com.MyAnimaLog.Veterinary.application.catalog.dto.UpdateClinicServiceRequest;
import com.MyAnimaLog.Veterinary.application.catalog.ports.out.ClinicServiceRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.domain.catalog.enums.ServiceCategory;
import com.MyAnimaLog.Veterinary.domain.catalog.exceptions.ClinicServiceNameAlreadyExistsException;
import com.MyAnimaLog.Veterinary.domain.catalog.exceptions.ClinicServiceNotFoundException;
import com.MyAnimaLog.Veterinary.domain.catalog.model.ClinicService;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.InsufficientPermissionException;
import com.MyAnimaLog.Veterinary.domain.security.Permission;
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
class UpdateClinicServiceServiceTest {

    @Mock
    private VeterinaryAuthorizationService authorizationService;
    @Mock
    private ClinicServiceRepositoryPort repositoryPort;

    @InjectMocks
    private UpdateClinicServiceService updateClinicServiceService;

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

    private void echoSave() {
        when(repositoryPort.save(any(ClinicService.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void update_shouldApplyOnlyProvidedFields() {
        when(repositoryPort.findByVeterinaryIdAndId(veterinaryId, serviceId)).thenReturn(Optional.of(existing));
        echoSave();

        ClinicServiceResponse response = updateClinicServiceService.update(veterinaryId, serviceId,
                UpdateClinicServiceRequest.builder().price(BigDecimal.valueOf(50000)).build());

        verify(authorizationService).require(veterinaryId, Permission.CLINIC_CONFIGURE);
        assertThat(response.getPrice()).isEqualByComparingTo("50000");
        assertThat(response.getName()).isEqualTo("Consulta general");
        assertThat(response.getDurationMinutes()).isEqualTo(30);
    }

    @Test
    void update_shouldThrowNotFound_whenServiceBelongsToAnotherClinic() {
        when(repositoryPort.findByVeterinaryIdAndId(veterinaryId, serviceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> updateClinicServiceService.update(veterinaryId, serviceId,
                UpdateClinicServiceRequest.builder().build()))
                .isInstanceOf(ClinicServiceNotFoundException.class);

        verify(repositoryPort, never()).save(any());
    }

    @Test
    void update_shouldNotConflict_whenNameIsResentUnchanged() {
        when(repositoryPort.findByVeterinaryIdAndId(veterinaryId, serviceId)).thenReturn(Optional.of(existing));
        echoSave();

        updateClinicServiceService.update(veterinaryId, serviceId,
                UpdateClinicServiceRequest.builder().name("Consulta general").build());

        verify(repositoryPort, never()).existsActiveByVeterinaryIdAndName(any(), any());
    }

    @Test
    void update_shouldThrowNameAlreadyExists_whenCollidesWithAnotherActiveService() {
        when(repositoryPort.findByVeterinaryIdAndId(veterinaryId, serviceId)).thenReturn(Optional.of(existing));
        when(repositoryPort.existsActiveByVeterinaryIdAndName(veterinaryId, "Vacunación")).thenReturn(true);

        assertThatThrownBy(() -> updateClinicServiceService.update(veterinaryId, serviceId,
                UpdateClinicServiceRequest.builder().name("Vacunación").build()))
                .isInstanceOf(ClinicServiceNameAlreadyExistsException.class);

        verify(repositoryPort, never()).save(any());
    }

    @Test
    void update_shouldPropagateInsufficientPermission_whenCallerLacksClinicConfigure() {
        doThrow(new InsufficientPermissionException(Permission.CLINIC_CONFIGURE))
                .when(authorizationService).require(veterinaryId, Permission.CLINIC_CONFIGURE);

        assertThatThrownBy(() -> updateClinicServiceService.update(veterinaryId, serviceId,
                UpdateClinicServiceRequest.builder().build()))
                .isInstanceOf(InsufficientPermissionException.class);

        verify(repositoryPort, never()).findByVeterinaryIdAndId(any(), any());
    }

    @Test
    void update_shouldAllowChangingAllowedRoles() {
        when(repositoryPort.findByVeterinaryIdAndId(veterinaryId, serviceId)).thenReturn(Optional.of(existing));
        echoSave();

        ClinicServiceResponse response = updateClinicServiceService.update(veterinaryId, serviceId,
                UpdateClinicServiceRequest.builder().allowedRoles(List.of("ASSISTANT")).build());

        assertThat(response.getAllowedRoles()).containsExactly(EmployeeRole.ASSISTANT.name());
    }
}

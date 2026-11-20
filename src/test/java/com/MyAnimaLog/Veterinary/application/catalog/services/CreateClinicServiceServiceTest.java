package com.MyAnimaLog.Veterinary.application.catalog.services;

import com.MyAnimaLog.Veterinary.application.catalog.dto.ClinicServiceResponse;
import com.MyAnimaLog.Veterinary.application.catalog.dto.CreateClinicServiceRequest;
import com.MyAnimaLog.Veterinary.application.catalog.ports.out.ClinicServiceRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.domain.catalog.exceptions.ClinicServiceNameAlreadyExistsException;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateClinicServiceServiceTest {

    @Mock
    private VeterinaryAuthorizationService authorizationService;
    @Mock
    private ClinicServiceRepositoryPort repositoryPort;

    @InjectMocks
    private CreateClinicServiceService createClinicServiceService;

    private UUID veterinaryId;
    private CreateClinicServiceRequest request;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();
        request = CreateClinicServiceRequest.builder()
                .name("Consulta general")
                .category("CONSULTATION")
                .durationMinutes(30)
                .price(BigDecimal.valueOf(40000))
                .build();
    }

    private void echoSave() {
        when(repositoryPort.save(any(ClinicService.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void create_shouldSaveWithDefaults_whenOptionalFieldsAreMissing() {
        echoSave();

        ClinicServiceResponse response = createClinicServiceService.create(veterinaryId, request);

        verify(authorizationService).require(veterinaryId, Permission.CLINIC_CONFIGURE);
        ArgumentCaptor<ClinicService> saved = ArgumentCaptor.forClass(ClinicService.class);
        verify(repositoryPort).save(saved.capture());
        assertThat(saved.getValue().getBookableOnline()).isTrue();
        assertThat(saved.getValue().getActive()).isTrue();
        assertThat(saved.getValue().getRequiresLicense()).isFalse();
        assertThat(saved.getValue().getAllowedRoles()).isEqualTo(ClinicServiceValidation.DEFAULT_ALLOWED_ROLES);
        assertThat(response.getName()).isEqualTo("Consulta general");
    }

    @Test
    void create_shouldThrowNameAlreadyExists_whenAnActiveServiceHasTheSameName() {
        when(repositoryPort.existsActiveByVeterinaryIdAndName(veterinaryId, "Consulta general")).thenReturn(true);

        assertThatThrownBy(() -> createClinicServiceService.create(veterinaryId, request))
                .isInstanceOf(ClinicServiceNameAlreadyExistsException.class);

        verify(repositoryPort, never()).save(any());
    }

    @Test
    void create_shouldPropagateInsufficientPermission_whenCallerLacksClinicConfigure() {
        doThrow(new InsufficientPermissionException(Permission.CLINIC_CONFIGURE))
                .when(authorizationService).require(veterinaryId, Permission.CLINIC_CONFIGURE);

        assertThatThrownBy(() -> createClinicServiceService.create(veterinaryId, request))
                .isInstanceOf(InsufficientPermissionException.class);

        verify(repositoryPort, never()).save(any());
    }

    @Test
    void create_shouldAcceptExplicitAllowedRoles() {
        request.setAllowedRoles(List.of("ASSISTANT", "RECEPTIONIST"));
        echoSave();

        createClinicServiceService.create(veterinaryId, request);

        ArgumentCaptor<ClinicService> saved = ArgumentCaptor.forClass(ClinicService.class);
        verify(repositoryPort).save(saved.capture());
        assertThat(saved.getValue().getAllowedRoles())
                .containsExactly(com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole.ASSISTANT,
                        com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole.RECEPTIONIST);
    }
}

package com.MyAnimaLog.Veterinary.application.clinic.services;

import com.MyAnimaLog.Veterinary.application.clinic.dto.UpdateVeterinarySettingsRequest;
import com.MyAnimaLog.Veterinary.application.clinic.dto.UpdateVeterinarySettingsResponse;
import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.InsufficientPermissionException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VeterinaryNotFoundException;
import com.MyAnimaLog.Veterinary.domain.security.Permission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateVeterinarySettingsServiceTest {

    @Mock
    private VeterinaryRepositoryPort veterinaryRepositoryPort;

    @Mock
    private VeterinaryAuthorizationService authorizationService;

    @InjectMocks
    private UpdateVeterinarySettingsService updateVeterinarySettingsService;

    private UUID veterinaryId;
    private Veterinary pendingVeterinary;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();
        // PENDING_DOCUMENTS / inactive on purpose: settings must be editable before approval.
        pendingVeterinary = Veterinary.builder()
                .id(veterinaryId)
                .status(VeterinaryStatus.PENDING_DOCUMENTS)
                .active(false)
                .timezone("America/Bogota")
                .currency("COP")
                .defaultAppointmentMinutes(30)
                .allowOnlineBooking(true)
                .bookingRequiresConfirmation(true)
                .cancellationMinHours(4)
                .directoryVisible(true)
                .build();

        lenient().when(veterinaryRepositoryPort.save(any(Veterinary.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void updateSettings_shouldSucceed_evenWhenVeterinaryIsPendingDocuments() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(pendingVeterinary));

        UpdateVeterinarySettingsResponse response = updateVeterinarySettingsService.updateSettings(
                veterinaryId,
                UpdateVeterinarySettingsRequest.builder().defaultAppointmentMinutes(45).build()
        );

        assertThat(response.getDefaultAppointmentMinutes()).isEqualTo(45);
    }

    @Test
    void updateSettings_shouldKeepExistingValues_whenFieldsAreNull() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(pendingVeterinary));

        updateVeterinarySettingsService.updateSettings(veterinaryId, UpdateVeterinarySettingsRequest.builder().build());

        verify(veterinaryRepositoryPort, times(1)).save(argThat(v ->
                v.getTimezone().equals("America/Bogota") && v.getCancellationMinHours() == 4
        ));
    }

    @Test
    void updateSettings_shouldThrowVeterinaryNotFoundException_whenVeterinaryDoesNotExist() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                updateVeterinarySettingsService.updateSettings(veterinaryId, UpdateVeterinarySettingsRequest.builder().build())
        ).isInstanceOf(VeterinaryNotFoundException.class);
    }

    @Test
    void updateSettings_shouldThrowInsufficientPermissionException_whenCallerLacksClinicConfigure() {
        doThrow(new InsufficientPermissionException(Permission.CLINIC_CONFIGURE))
                .when(authorizationService).require(veterinaryId, Permission.CLINIC_CONFIGURE);

        assertThatThrownBy(() ->
                updateVeterinarySettingsService.updateSettings(veterinaryId, UpdateVeterinarySettingsRequest.builder().build())
        ).isInstanceOf(InsufficientPermissionException.class);

        verify(veterinaryRepositoryPort, never()).save(any());
    }
}

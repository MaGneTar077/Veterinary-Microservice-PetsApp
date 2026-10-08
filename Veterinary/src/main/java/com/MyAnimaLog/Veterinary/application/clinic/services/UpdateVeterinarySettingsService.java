package com.MyAnimaLog.Veterinary.application.clinic.services;

import com.MyAnimaLog.Veterinary.application.clinic.dto.UpdateVeterinarySettingsRequest;
import com.MyAnimaLog.Veterinary.application.clinic.dto.UpdateVeterinarySettingsResponse;
import com.MyAnimaLog.Veterinary.application.clinic.ports.in.UpdateVeterinarySettingsUseCase;
import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.security.Permission;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VeterinaryNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdateVeterinarySettingsService implements UpdateVeterinarySettingsUseCase {

    private final VeterinaryRepositoryPort veterinaryRepositoryPort;
    private final VeterinaryAuthorizationService authorizationService;

    @Override
    public UpdateVeterinarySettingsResponse updateSettings(UUID veterinaryId, UpdateVeterinarySettingsRequest request) {
        authorizationService.require(veterinaryId, Permission.CLINIC_CONFIGURE);

        Veterinary veterinary = veterinaryRepositoryPort.findById(veterinaryId)
                .orElseThrow(VeterinaryNotFoundException::new);

        Veterinary updated = veterinary.toBuilder()
                .timezone(request.getTimezone() != null ? request.getTimezone() : veterinary.getTimezone())
                .defaultAppointmentMinutes(request.getDefaultAppointmentMinutes() != null
                        ? request.getDefaultAppointmentMinutes() : veterinary.getDefaultAppointmentMinutes())
                .allowOnlineBooking(request.getAllowOnlineBooking() != null
                        ? request.getAllowOnlineBooking() : veterinary.getAllowOnlineBooking())
                .bookingRequiresConfirmation(request.getBookingRequiresConfirmation() != null
                        ? request.getBookingRequiresConfirmation() : veterinary.getBookingRequiresConfirmation())
                .cancellationMinHours(request.getCancellationMinHours() != null
                        ? request.getCancellationMinHours() : veterinary.getCancellationMinHours())
                .directoryVisible(request.getDirectoryVisible() != null
                        ? request.getDirectoryVisible() : veterinary.getDirectoryVisible())
                .build();

        Veterinary saved = veterinaryRepositoryPort.save(updated);

        return UpdateVeterinarySettingsResponse.builder()
                .id(saved.getId())
                .timezone(saved.getTimezone())
                .defaultAppointmentMinutes(saved.getDefaultAppointmentMinutes())
                .allowOnlineBooking(saved.getAllowOnlineBooking())
                .bookingRequiresConfirmation(saved.getBookingRequiresConfirmation())
                .cancellationMinHours(saved.getCancellationMinHours())
                .directoryVisible(saved.getDirectoryVisible())
                .build();
    }
}

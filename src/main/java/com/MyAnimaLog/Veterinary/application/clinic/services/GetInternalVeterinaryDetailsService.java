package com.MyAnimaLog.Veterinary.application.clinic.services;

import com.MyAnimaLog.Veterinary.application.clinic.dto.GetInternalVeterinaryDetailsResponse;
import com.MyAnimaLog.Veterinary.application.clinic.ports.in.GetInternalVeterinaryDetailsUseCase;
import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VeterinaryNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Datos de la clínica para Calendar (pedido 1 de CALENDAR_REQUESTS_TO_VETERINARY.md): decidir si
 * acepta reservas en línea, cuándo se puede cancelar y en qué zona horaria mostrar las horas.
 * {@code /internal/veterinaries/{id}/summary} (Medical) no se toca — este es un endpoint nuevo y
 * distinto, más completo pero sin ningún dato tributario/personal.
 */
@Service
@RequiredArgsConstructor
public class GetInternalVeterinaryDetailsService implements GetInternalVeterinaryDetailsUseCase {

    private final VeterinaryRepositoryPort veterinaryRepositoryPort;

    @Override
    public GetInternalVeterinaryDetailsResponse get(UUID veterinaryId) {
        Veterinary clinic = veterinaryRepositoryPort.findById(veterinaryId)
                .orElseThrow(VeterinaryNotFoundException::new);

        return GetInternalVeterinaryDetailsResponse.builder()
                .id(clinic.getId())
                .name(clinic.getName())
                .status(clinic.getStatus())
                .allowOnlineBooking(clinic.getAllowOnlineBooking())
                .bookingRequiresConfirmation(clinic.getBookingRequiresConfirmation())
                .cancellationMinHours(clinic.getCancellationMinHours())
                .timezone(clinic.getTimezone())
                .address(clinic.getAddress())
                .phone(clinic.getPhone())
                .build();
    }
}

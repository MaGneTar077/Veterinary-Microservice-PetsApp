package com.MyAnimaLog.Veterinary.application.patients.services;

import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.patients.dto.GetVeterinarySummaryResponse;
import com.MyAnimaLog.Veterinary.application.patients.ports.in.GetVeterinarySummaryUseCase;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VeterinaryNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Endpoint interno {@code GET /internal/veterinaries/{id}/summary}. Medical lo usa para poner el
 * nombre de la clínica en los eventos de notificación; si falla, Medical omite el nombre y sigue
 * publicando (VETERINARY_REQUERIMIENTOS_DESDE_MEDICAL.md §3.2). A diferencia de {@code access},
 * aquí sí responde {@code 404} si la clínica no existe: no hay ningún "acceso" que degradar a
 * {@code false}, y el doc de Medical lo pide así explícitamente.
 */
@Service
@RequiredArgsConstructor
public class GetVeterinarySummaryService implements GetVeterinarySummaryUseCase {

    private final VeterinaryRepositoryPort veterinaryRepositoryPort;

    @Override
    public GetVeterinarySummaryResponse getSummary(UUID veterinaryId) {
        Veterinary veterinary = veterinaryRepositoryPort.findById(veterinaryId)
                .orElseThrow(VeterinaryNotFoundException::new);

        return GetVeterinarySummaryResponse.builder()
                .id(veterinary.getId())
                .name(veterinary.getName())
                .timezone(veterinary.getTimezone())
                .status(veterinary.getStatus())
                .logoUrl(veterinary.getLogoUrl())
                .build();
    }
}

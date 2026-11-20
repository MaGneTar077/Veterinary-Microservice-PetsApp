package com.MyAnimaLog.Veterinary.application.catalog.ports.in;

import com.MyAnimaLog.Veterinary.application.catalog.dto.InternalClinicServiceResponse;

import java.util.List;
import java.util.UUID;

public interface GetInternalClinicServicesUseCase {

    /**
     * @param bookableOnlineFilter como texto crudo del query param: {@code null} si no vino (no
     *                             filtra), o {@code "true"}/{@code "false"} — cualquier otro valor
     *                             es {@code 400 VALIDATION_ERROR}. Se valida acá (no en el
     *                             controlador) para que la regla sea testeable sin MockMvc.
     */
    List<InternalClinicServiceResponse> list(UUID veterinaryId, boolean activeOnly, String bookableOnlineFilter);
}

package com.MyAnimaLog.Veterinary.application.catalog.ports.out;

import com.MyAnimaLog.Veterinary.domain.catalog.model.ClinicService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClinicServiceRepositoryPort {

    ClinicService save(ClinicService service);

    /** Escopado por clínica: un id de otra clínica no debe poder resolverse desde acá. */
    Optional<ClinicService> findByVeterinaryIdAndId(UUID veterinaryId, UUID serviceId);

    List<ClinicService> findByVeterinaryId(UUID veterinaryId);

    List<ClinicService> findByVeterinaryIdAndActiveTrue(UUID veterinaryId);

    /** Respeta el índice único parcial {@code ux_clinic_services_vet_name_active}. */
    boolean existsActiveByVeterinaryIdAndName(UUID veterinaryId, String name);
}

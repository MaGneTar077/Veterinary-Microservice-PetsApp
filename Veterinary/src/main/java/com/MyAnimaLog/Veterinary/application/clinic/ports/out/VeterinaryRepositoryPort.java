package com.MyAnimaLog.Veterinary.application.clinic.ports.out;

import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VeterinaryRepositoryPort {
    Veterinary save(Veterinary veterinary);
    boolean existsByEmail(String email);
    boolean existsByName(String name);
    Optional<Veterinary> findById(UUID id);
    boolean existsByInviteCode(String inviteCode);
    boolean existsByEmailAndIdNot(String email, UUID id);
    Optional<Veterinary> findByInviteCode(String inviteCode);
    Optional<Veterinary> findByInviteLink(String inviteLink);
    boolean existsByNit(String nit);

    /** Page of clinics for the admin panel (Fase 2B Parte 4B), filtered and ordered by {@code createdAt} desc. */
    AdminClinicPage findForAdminList(VeterinaryStatus status, String search, String plan, String city,
                                      String department, int page, int size);

    record AdminClinicPage(List<Veterinary> items, long total) {
    }
}

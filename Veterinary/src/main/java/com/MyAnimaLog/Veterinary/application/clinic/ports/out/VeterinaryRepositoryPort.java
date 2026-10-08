package com.MyAnimaLog.Veterinary.application.clinic.ports.out;

import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;

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
}

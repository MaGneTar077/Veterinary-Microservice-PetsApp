package com.MyAnimaLog.Veterinary.application.patients.dto;

import com.MyAnimaLog.Veterinary.domain.patients.enums.LinkStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * {@code alreadyLinked = true} significa que la vinculación ya existía y no se hizo nada: la
 * operación es idempotente y responde {@code 200}, no {@code 409}. El frontend puede reintentar un
 * POST que no sabe si llegó (una conexión que se cortó, un doble tap) sin tener que distinguir un
 * error real de "ya estaba".
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LinkToVeterinaryResponse {
    private UUID id;
    private UUID veterinaryId;
    private String veterinaryName;
    private String logoUrl;
    private LinkStatus status;
    private LocalDateTime linkedAt;
    private boolean alreadyLinked;
}

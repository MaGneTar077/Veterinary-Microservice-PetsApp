package com.MyAnimaLog.Veterinary.application.shared.ports.out;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lo que Veterinary necesita saber de Pets. Veterinary <strong>no guarda</strong> la mascota ni su
 * dueño: guarda el consentimiento ({@code veterinary_patients}) y le pregunta a Pets todo lo demás
 * cuando lo necesita.
 *
 * <p>A diferencia de {@link UserServicePort#getUsers(List)}, los métodos de acá
 * <strong>sí lanzan</strong> {@code ExternalServiceUnavailableException} (503) cuando Pets no
 * responde, y eso es a propósito: no son para enriquecer una respuesta, son para
 * <strong>autorizar</strong> (¿es esta persona la dueña de la mascota?) y para datos sin los cuales
 * la respuesta sería incorrecta. Degradar a "no sé" sería dejar pasar o mostrar datos falsos —
 * CONTRATOS_COMPARTIDOS.md §2: "si el servicio llamado no responde, el que llama responde 503,
 * nunca 'deja pasar'".
 */
public interface PetsServicePort {

    /**
     * {@code GET /internal/pets/{petId}/owner/{userId}} → {@code {isOwner}}. Para una sola mascota;
     * cuando son varias conviene {@link #getPets(List)}, que trae el {@code ownerId} de todas en una
     * llamada.
     */
    boolean isOwner(UUID petId, UUID userId);

    /**
     * {@code POST /internal/pets/batch} con {@code {ids}} → los datos de esas mascotas, indexados
     * por id. <strong>Una sola llamada por página</strong>, nunca una por fila.
     *
     * <p>Un id que Pets no conozca simplemente no aparece en el mapa; el caller decide qué
     * significa eso (al compartir, que falte es tan rechazable como que sea de otra persona).
     */
    Map<UUID, PetInfo> getPets(List<UUID> petIds);

    /**
     * {@code ownerId} es {@code null} en una mascota registrada por una clínica que todavía nadie
     * reclamó (Fase 4B).
     *
     * <p>No incluye el {@code ownerContact} que Pets también devuelve: solo lo necesitan los
     * pacientes walk-in de la Fase 4B y su forma exacta no está acordada todavía. Jackson ignora las
     * propiedades que no estén acá, así que agregarlo después no rompe nada.
     */
    record PetInfo(UUID id, UUID ownerId, String name, String species, String breed, String sex,
                    LocalDate birthDate, String photoUrl, UUID createdByVeterinaryId,
                    String claimStatus) {
    }
}

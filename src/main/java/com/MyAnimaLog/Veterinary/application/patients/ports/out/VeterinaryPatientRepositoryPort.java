package com.MyAnimaLog.Veterinary.application.patients.ports.out;

import com.MyAnimaLog.Veterinary.domain.patients.model.VeterinaryPatient;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VeterinaryPatientRepositoryPort {

    VeterinaryPatient save(VeterinaryPatient patient);

    /** La fila del par, en cualquier estado de consentimiento (hay como máximo una). */
    Optional<VeterinaryPatient> findByVeterinaryIdAndPetId(UUID veterinaryId, UUID petId);

    /**
     * Las mascotas que <strong>ese dueño</strong> comparte activamente con <strong>esa
     * clínica</strong>. Es lo que hay que revocar al desvincularse, y lo que se cuenta para mostrar
     * "comparte N mascotas" en el listado de clientes.
     *
     * <p>Devuelve las filas y no un {@code int} de un {@code UPDATE} masivo a propósito: la
     * revocación en cascada guarda cada fila por el camino normal del {@code save}, para que
     * {@code @UpdateTimestamp} mantenga {@code updated_at}. Un {@code UPDATE} en JPQL lo
     * <strong>saltearía en silencio</strong> (Hibernate no aplica las anotaciones de generación a
     * las sentencias masivas), y acá N es chico —las mascotas que una persona comparte con una sola
     * clínica—, así que no vale la pena ese riesgo.
     */
    List<VeterinaryPatient> findGrantedByVeterinaryIdAndGrantedBy(UUID veterinaryId, UUID grantedBy);

    /** Cuántas mascotas comparte activamente cada dueño con una clínica, para el listado de clientes. */
    long countGrantedByVeterinaryIdAndGrantedBy(UUID veterinaryId, UUID grantedBy);
}

package com.MyAnimaLog.Veterinary.application.patients.ports.out;

import com.MyAnimaLog.Veterinary.domain.patients.model.UserVeterinaryLink;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserVeterinaryLinkRepositoryPort {

    UserVeterinaryLink save(UserVeterinaryLink link);

    /**
     * La vinculación <strong>activa</strong> del par, si hay. El índice único parcial
     * {@code ux_link_active_user_vet} garantiza que haya como máximo una, así que devolver
     * {@code Optional} es seguro.
     */
    Optional<UserVeterinaryLink> findActiveLink(UUID userId, UUID veterinaryId);

    /**
     * La fila más reciente del par, en cualquier estado, para reactivarla al re-vincularse en vez de
     * insertar otra.
     *
     * <p>Es deliberadamente "la más reciente" y no "la fila": la tabla no tuvo ningún índice único
     * hasta {@code 008_patients.sql}, así que puede traer varias filas {@code UNLINKED} del mismo par
     * (las que ese script dedupicó). Por eso tampoco existe acá un
     * {@code findByUserIdAndVeterinaryId} que devuelva {@code Optional}: con datos viejos reventaría
     * con más de un resultado.
     */
    Optional<UserVeterinaryLink> findLatestLink(UUID userId, UUID veterinaryId);

    /** Las clínicas donde el usuario está vinculado activamente (su selector de clínicas). */
    List<UserVeterinaryLink> findActiveByUserId(UUID userId);

    /** Página de clientes activos de una clínica (`ListClients`), para su tenant únicamente. */
    ClientsPage findActiveByVeterinaryId(UUID veterinaryId, int page, int size);

    record ClientsPage(List<UserVeterinaryLink> items, long total) {
    }
}

package com.MyAnimaLog.Veterinary.application.shared.ports.out;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface UserServicePort {
    UserInfo getUser(UUID userId);

    /**
     * Resolución de varios usuarios a la vez, indexada por id. A diferencia de {@link #getUser(UUID)},
     * <strong>nunca lanza</strong>: los ids que el User service no pueda resolver simplemente no
     * aparecen en el mapa, para que una caída suya no tumbe una operación que solo usa estos datos
     * para enriquecer una respuesta o para una validación best-effort. Todo caller debe funcionar
     * con un mapa incompleto.
     *
     * <p>Hoy el adaptador hace una llamada HTTP por id (el User service no expone un endpoint en
     * lote todavía — ver "Pendientes del lado del User service" en CLAUDE.md), así que conviene
     * solo con listas cortas.
     */
    Map<UUID, UserInfo> getUsers(List<UUID> userIds);

    /**
     * {@code name} is best-effort: the User service's {@code /internal/users/{id}} contract does not
     * document a name field today, so this is {@code null} until that's confirmed/added on their side —
     * every caller must fall back to another source (e.g. {@code Veterinary.legalRepName}) when it's null.
     */
    record UserInfo(UUID id, String email, Boolean emailVerified, String name) {
    }
}

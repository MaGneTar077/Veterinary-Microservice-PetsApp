package com.MyAnimaLog.Veterinary.application.staff.services;

import com.MyAnimaLog.Veterinary.application.shared.ports.out.UserServicePort;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Resuelve {@code userId -> UserInfo} llamando a {@link UserServicePort#getUser(UUID)}
 * <strong>en paralelo</strong> para cada id distinto, con un presupuesto de tiempo total para toda
 * la lista — no por llamada, porque {@code getUser} ya tiene su propio timeout HTTP de 3 s y esta
 * lista puede tener varios ids. Si el presupuesto se agota, los ids cuya llamada todavía no
 * terminó quedan en {@code null} (nunca se inventa un nombre ni se bloquea más allá del
 * presupuesto). Utilidad estática, no un puerto ni un servicio: no tiene estado propio.
 *
 * <p>Usada por {@code GetInternalVeterinaryMembersService} para que Calendar reciba una respuesta
 * dentro de los 3 s que espera, incluso si el User service está lento para alguno de los miembros.
 */
public final class BestEffortUserNames {

    private BestEffortUserNames() {
    }

    public static Map<UUID, UserServicePort.UserInfo> resolve(UserServicePort userServicePort,
                                                                List<UUID> userIds, Duration budget) {
        List<UUID> distinct = userIds.stream().distinct().toList();
        if (distinct.isEmpty()) {
            return Map.of();
        }

        Map<UUID, CompletableFuture<UserServicePort.UserInfo>> futures = new HashMap<>();
        for (UUID userId : distinct) {
            futures.put(userId, CompletableFuture.supplyAsync(() -> {
                try {
                    return userServicePort.getUser(userId);
                } catch (RuntimeException ex) {
                    return null;
                }
            }));
        }

        try {
            CompletableFuture.allOf(futures.values().toArray(new CompletableFuture[0]))
                    .get(budget.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException ignored) {
            // Presupuesto agotado: lo que no haya terminado queda null abajo, no se espera más.
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException ignored) {
            // No debería pasar (cada future atrapa su propia RuntimeException), pero por si acaso
            // no se propaga: un fallo de nombre nunca debe tumbar la respuesta completa.
        }

        Map<UUID, UserServicePort.UserInfo> result = new HashMap<>();
        futures.forEach((userId, future) -> result.put(userId, future.isDone() ? future.getNow(null) : null));
        return result;
    }
}

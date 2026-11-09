package com.MyAnimaLog.Veterinary.infrastructure.clients;

import com.MyAnimaLog.Veterinary.application.shared.ports.out.PetsServicePort;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.ExternalServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * {@link PetsServicePort} sobre la API interna de Pets, con {@code X-Internal-Api-Key} y timeout de
 * 3 s (CONTRATOS_COMPARTIDOS.md §2). Mismo patrón que {@link UserServiceRestAdapter}.
 *
 * <p>Cualquier fallo se traduce a {@code ExternalServiceUnavailableException} (503) y
 * <strong>nunca</strong> a un resultado vacío o a {@code false}: estas respuestas deciden si una
 * clínica puede ver la historia clínica de una mascota, así que un Pets caído tiene que cortar la
 * operación, no convertirse en un "no es tu mascota" (que daría un 403 engañoso) ni en un
 * "sí, adelante".
 */
@Component
public class PetsServiceRestAdapter implements PetsServicePort {

    private static final Logger log = LoggerFactory.getLogger(PetsServiceRestAdapter.class);
    private static final String HEADER = "X-Internal-Api-Key";

    private final RestClient restClient;

    public PetsServiceRestAdapter(@Value("${app.pets-service.base-url}") String baseUrl,
                                   @Value("${app.security.internal-api-key}") String internalApiKey) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HEADER, internalApiKey)
                .requestFactory(requestFactory())
                .build();
    }

    private static SimpleClientHttpRequestFactory requestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3_000);
        factory.setReadTimeout(3_000);
        return factory;
    }

    @Override
    public boolean isOwner(UUID petId, UUID userId) {
        try {
            OwnerResponse response = restClient.get()
                    .uri("/internal/pets/{petId}/owner/{userId}", petId, userId)
                    .retrieve()
                    .body(OwnerResponse.class);
            if (response == null || response.isOwner() == null) {
                throw new ExternalServiceUnavailableException("Pets service returned an empty body");
            }
            return response.isOwner();
        } catch (RestClientException ex) {
            // Sin el petId/userId en el log no hay forma de investigar un 503 reportado por un
            // usuario; no son datos sensibles (son identificadores opacos).
            log.error("Pets service ownership check failed for petId={} userId={}", petId, userId, ex);
            throw new ExternalServiceUnavailableException("Pets service lookup failed", ex);
        }
    }

    @Override
    public Map<UUID, PetInfo> getPets(List<UUID> petIds) {
        if (petIds == null || petIds.isEmpty()) {
            // Sin ids no hay nada que preguntar: evita una llamada HTTP que Pets respondería vacía.
            return Map.of();
        }
        List<UUID> ids = petIds.stream().distinct().toList();
        try {
            PetInfo[] response = restClient.post()
                    .uri("/internal/pets/batch")
                    .body(new BatchRequest(ids))
                    .retrieve()
                    .body(PetInfo[].class);
            if (response == null) {
                throw new ExternalServiceUnavailableException("Pets service returned an empty body");
            }
            Map<UUID, PetInfo> byId = new LinkedHashMap<>();
            for (PetInfo pet : response) {
                if (pet != null && pet.id() != null) {
                    byId.put(pet.id(), pet);
                }
            }
            return byId;
        } catch (RestClientException ex) {
            log.error("Pets service batch lookup failed for {} ids", ids.size(), ex);
            throw new ExternalServiceUnavailableException("Pets service lookup failed", ex);
        }
    }

    private record BatchRequest(List<UUID> ids) {
    }

    /** {@code Boolean} y no {@code boolean}: así un cuerpo sin el campo no se lee como {@code false}. */
    private record OwnerResponse(Boolean isOwner) {
    }
}

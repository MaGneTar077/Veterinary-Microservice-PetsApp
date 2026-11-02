package com.MyAnimaLog.Veterinary.infrastructure.clients;

import com.MyAnimaLog.Veterinary.application.shared.ports.out.UserServicePort;
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

@Component
public class UserServiceRestAdapter implements UserServicePort {

    private static final Logger log = LoggerFactory.getLogger(UserServiceRestAdapter.class);
    private static final String HEADER = "X-Internal-Api-Key";

    private final RestClient restClient;

    public UserServiceRestAdapter(@Value("${app.user-service.base-url}") String baseUrl,
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
    public UserInfo getUser(UUID userId) {
        try {
            UserResponse response = restClient.get()
                    .uri("/internal/users/{id}", userId)
                    .retrieve()
                    .body(UserResponse.class);
            if (response == null) {
                throw new ExternalServiceUnavailableException("User service returned an empty body");
            }
            return new UserInfo(userId, response.email(), response.emailVerified(), response.name());
        } catch (RestClientException ex) {
            log.error("User service lookup failed for userId={}", userId, ex);
            throw new ExternalServiceUnavailableException("User service lookup failed", ex);
        }
    }

    @Override
    public Map<UUID, UserInfo> getUsers(List<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, UserInfo> resolved = new LinkedHashMap<>();
        for (UUID userId : userIds.stream().distinct().toList()) {
            try {
                resolved.put(userId, getUser(userId));
            } catch (ExternalServiceUnavailableException ex) {
                // Best-effort por contrato del puerto: el id queda fuera del mapa y el caller decide.
                log.warn("User service lookup skipped for userId={}", userId);
            }
        }
        return resolved;
    }

    private record UserResponse(UUID id, String email, Boolean emailVerified, String name) {
    }
}

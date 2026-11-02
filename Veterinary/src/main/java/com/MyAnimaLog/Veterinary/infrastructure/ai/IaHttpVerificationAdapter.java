package com.MyAnimaLog.Veterinary.infrastructure.ai;

import com.MyAnimaLog.Veterinary.application.verification.ports.out.IaVerificationPort;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.ExternalServiceUnavailableException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.UUID;

/**
 * Real adapter for the IA verification service (VETERINARY_SERVICE_PLAN.md §12, D10):
 * {@code POST {IA_SERVICE_URL}/internal/verification/analyze}, authenticated the same way every
 * other inter-service call in this codebase is ({@code X-Internal-Api-Key}), documents passed as
 * 10-minute signed URLs (never the files themselves), 60 s timeout. Never logs the request or
 * response bodies — they may contain extracted personal data.
 */
@Component
@ConditionalOnProperty(name = "app.ai.verification-enabled", havingValue = "true")
public class IaHttpVerificationAdapter implements IaVerificationPort {

    private static final Logger log = LoggerFactory.getLogger(IaHttpVerificationAdapter.class);
    private static final String HEADER = "X-Internal-Api-Key";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public IaHttpVerificationAdapter(@Value("${app.ai.service-url}") String baseUrl,
                                      @Value("${app.security.internal-api-key}") String internalApiKey,
                                      ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HEADER, internalApiKey)
                .requestFactory(requestFactory())
                .build();
    }

    private static SimpleClientHttpRequestFactory requestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(60_000);
        factory.setReadTimeout(60_000);
        return factory;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public IaAnalysisResult analyze(IaAnalysisRequest request) {
        try {
            String rawBody = restClient.post()
                    .uri("/internal/verification/analyze")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(toRequestBody(request))
                    .retrieve()
                    .body(String.class);
            if (rawBody == null || rawBody.isBlank()) {
                throw new ExternalServiceUnavailableException("IA service returned an empty body");
            }
            JsonNode node = objectMapper.readTree(rawBody);
            String recommendation = node.path("recommendation").isMissingNode() ? null : node.path("recommendation").asText(null);
            String riskLevel = node.path("riskLevel").isMissingNode() ? null : node.path("riskLevel").asText(null);
            return new IaAnalysisResult(recommendation, riskLevel, rawBody);
        } catch (RestClientException | com.fasterxml.jackson.core.JsonProcessingException ex) {
            log.error("IA service analyze call failed for requestId={}", request.requestId());
            throw new ExternalServiceUnavailableException("IA service analyze call failed", ex);
        }
    }

    private AnalyzeRequestBody toRequestBody(IaAnalysisRequest request) {
        List<DocumentRefBody> documents = request.documents().stream()
                .map(doc -> new DocumentRefBody(doc.documentType(), doc.signedUrl()))
                .toList();
        return new AnalyzeRequestBody(request.requestId(), request.subjectType().name(), documents);
    }

    private record AnalyzeRequestBody(UUID requestId, String subjectType, List<DocumentRefBody> documents) {
    }

    private record DocumentRefBody(String documentType, String signedUrl) {
    }
}

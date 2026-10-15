package com.MyAnimaLog.Veterinary.infrastructure.storage;

import com.MyAnimaLog.Veterinary.application.shared.ports.out.FileStoragePort;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.ExternalServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class SupabaseStorageAdapter implements FileStoragePort {

    private static final Logger log = LoggerFactory.getLogger(SupabaseStorageAdapter.class);

    private final RestClient restClient;
    private final String storagePublicBaseUrl;

    public SupabaseStorageAdapter(@Value("${supabase.url}") String supabaseUrl,
                                   @Value("${supabase.key}") String supabaseKey) {
        this.storagePublicBaseUrl = supabaseUrl + "/storage/v1";
        this.restClient = RestClient.builder()
                .baseUrl(storagePublicBaseUrl)
                .defaultHeader("apikey", supabaseKey)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + supabaseKey)
                .requestFactory(requestFactory())
                .build();
    }

    private static SimpleClientHttpRequestFactory requestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(10_000);
        return factory;
    }

    @Override
    public String uploadPublic(String bucket, String path, byte[] bytes, String contentType) {
        upload(bucket, path, bytes, contentType);
        return storagePublicBaseUrl + "/object/public/" + bucket + "/" + path;
    }

    @Override
    public void uploadPrivate(String bucket, String path, byte[] bytes, String contentType) {
        upload(bucket, path, bytes, contentType);
    }

    private void upload(String bucket, String path, byte[] bytes, String contentType) {
        try {
            restClient.post()
                    .uri("/object/" + bucket + "/" + path)
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .body(bytes)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            log.error("Supabase storage upload failed for bucket={}, path={}", bucket, path);
            throw new ExternalServiceUnavailableException("Supabase storage upload failed", ex);
        }
    }

    @Override
    public String createSignedUrl(String bucket, String path, Duration ttl) {
        try {
            SignedUrlResponse response = restClient.post()
                    .uri("/object/sign/" + bucket + "/" + path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("expiresIn", ttl.toSeconds()))
                    .retrieve()
                    .body(SignedUrlResponse.class);
            if (response == null || response.signedURL() == null) {
                throw new ExternalServiceUnavailableException("Supabase storage returned an empty signed URL");
            }
            return storagePublicBaseUrl + response.signedURL();
        } catch (RestClientException ex) {
            log.error("Supabase storage signed URL request failed for bucket={}, path={}", bucket, path);
            throw new ExternalServiceUnavailableException("Supabase storage signed URL request failed", ex);
        }
    }

    @Override
    public void delete(String bucket, String path) {
        try {
            restClient.method(HttpMethod.DELETE)
                    .uri("/object/" + bucket)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("prefixes", List.of(path)))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            log.error("Supabase storage delete failed for bucket={}, path={}", bucket, path);
            throw new ExternalServiceUnavailableException("Supabase storage delete failed", ex);
        }
    }

    private record SignedUrlResponse(String signedURL) {
    }
}

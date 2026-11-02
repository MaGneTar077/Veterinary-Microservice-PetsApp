package com.MyAnimaLog.Veterinary.infrastructure.storage;

import com.MyAnimaLog.Veterinary.domain.shared.exceptions.ExternalServiceUnavailableException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SupabaseStorageAdapterTest {

    private HttpServer server;
    private SupabaseStorageAdapter adapter;
    private final Deque<RecordedRequest> requests = new ArrayDeque<>();
    private int nextStatus = 200;
    private String nextResponseBody = "{}";

    private record RecordedRequest(String method, String path, String body) {
    }

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            byte[] requestBytes = exchange.getRequestBody().readAllBytes();
            requests.add(new RecordedRequest(
                    exchange.getRequestMethod(),
                    exchange.getRequestURI().toString(),
                    new String(requestBytes, StandardCharsets.UTF_8)));

            byte[] responseBytes = nextResponseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(nextStatus, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        });
        server.start();

        String baseUrl = "http://localhost:" + server.getAddress().getPort();
        adapter = new SupabaseStorageAdapter(baseUrl, "test-service-key");
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void uploadPublic_shouldPostBytesAndReturnPublicUrl() {
        String result = adapter.uploadPublic("veterinary-logos", "veterinaries/1/logo.png",
                "hello".getBytes(StandardCharsets.UTF_8), "image/png");

        assertThat(result).isEqualTo("http://localhost:" + server.getAddress().getPort()
                + "/storage/v1/object/public/veterinary-logos/veterinaries/1/logo.png");

        RecordedRequest request = requests.poll();
        assertThat(request.method()).isEqualTo("POST");
        assertThat(request.path()).isEqualTo("/storage/v1/object/veterinary-logos/veterinaries/1/logo.png");
        assertThat(request.body()).isEqualTo("hello");
    }

    @Test
    void uploadPrivate_shouldPostBytes_andReturnNothing() {
        adapter.uploadPrivate("verification-documents", "verifications/1/doc.pdf",
                "data".getBytes(StandardCharsets.UTF_8), "application/pdf");

        RecordedRequest request = requests.poll();
        assertThat(request.method()).isEqualTo("POST");
        assertThat(request.path()).isEqualTo("/storage/v1/object/verification-documents/verifications/1/doc.pdf");
    }

    @Test
    void createSignedUrl_shouldReturnFullUrl_fromRelativeSignedUrlResponse() {
        nextResponseBody = "{\"signedURL\":\"/object/sign/verification-documents/verifications/1/doc.pdf?token=abc\"}";

        String result = adapter.createSignedUrl("verification-documents", "verifications/1/doc.pdf", Duration.ofMinutes(10));

        assertThat(result).isEqualTo("http://localhost:" + server.getAddress().getPort()
                + "/storage/v1/object/sign/verification-documents/verifications/1/doc.pdf?token=abc");

        RecordedRequest request = requests.poll();
        assertThat(request.method()).isEqualTo("POST");
        assertThat(request.body()).contains("\"expiresIn\":600");
    }

    @Test
    void delete_shouldSendPrefixesBody() {
        adapter.delete("verification-documents", "verifications/1/doc.pdf");

        RecordedRequest request = requests.poll();
        assertThat(request.method()).isEqualTo("DELETE");
        assertThat(request.path()).isEqualTo("/storage/v1/object/verification-documents");
        assertThat(request.body()).contains("verifications/1/doc.pdf");
    }

    @Test
    void uploadPublic_shouldThrowExternalServiceUnavailable_onServerError() {
        nextStatus = 500;
        nextResponseBody = "{\"error\":\"boom\"}";

        assertThatThrownBy(() -> adapter.uploadPublic("veterinary-logos", "x/y.png",
                "x".getBytes(StandardCharsets.UTF_8), "image/png"))
                .isInstanceOf(ExternalServiceUnavailableException.class);
    }

    @Test
    void createSignedUrl_shouldThrowExternalServiceUnavailable_onServerError() {
        nextStatus = 503;

        assertThatThrownBy(() -> adapter.createSignedUrl("verification-documents", "x/y.pdf", Duration.ofMinutes(10)))
                .isInstanceOf(ExternalServiceUnavailableException.class);
    }
}

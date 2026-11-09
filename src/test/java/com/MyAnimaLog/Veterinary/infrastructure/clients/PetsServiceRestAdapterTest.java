package com.MyAnimaLog.Veterinary.infrastructure.clients;

import com.MyAnimaLog.Veterinary.application.shared.ports.out.PetsServicePort;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.ExternalServiceUnavailableException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Levanta un {@code HttpServer} real en un puerto libre (no llama a Pets) y verifica
 * método/ruta/cuerpo de cada operación más el mapeo a {@code ExternalServiceUnavailableException} —
 * mismo enfoque que {@code SupabaseStorageAdapterTest}. Con mocks no se podría ver que la ruta o el
 * cuerpo del batch estén mal armados.
 */
class PetsServiceRestAdapterTest {

    private HttpServer server;
    private PetsServiceRestAdapter adapter;
    private final Deque<RecordedRequest> requests = new ArrayDeque<>();
    private int nextStatus = 200;
    private String nextResponseBody = "{}";

    private record RecordedRequest(String method, String path, String body, String apiKey) {
    }

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            byte[] requestBytes = exchange.getRequestBody().readAllBytes();
            requests.add(new RecordedRequest(
                    exchange.getRequestMethod(),
                    exchange.getRequestURI().toString(),
                    new String(requestBytes, StandardCharsets.UTF_8),
                    exchange.getRequestHeaders().getFirst("X-Internal-Api-Key")));

            byte[] responseBytes = nextResponseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(nextStatus, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        });
        server.start();

        adapter = new PetsServiceRestAdapter(
                "http://localhost:" + server.getAddress().getPort(), "test-internal-key");
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void isOwner_shouldCallTheInternalOwnerEndpoint_andSendTheApiKey() {
        UUID petId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        nextResponseBody = "{\"isOwner\":true}";

        assertThat(adapter.isOwner(petId, userId)).isTrue();

        RecordedRequest request = requests.poll();
        assertThat(request.method()).isEqualTo("GET");
        assertThat(request.path()).isEqualTo("/internal/pets/" + petId + "/owner/" + userId);
        assertThat(request.apiKey()).isEqualTo("test-internal-key");
    }

    @Test
    void isOwner_shouldReturnFalse_whenPetsSaysSo() {
        nextResponseBody = "{\"isOwner\":false}";

        assertThat(adapter.isOwner(UUID.randomUUID(), UUID.randomUUID())).isFalse();
    }

    @Test
    void isOwner_shouldThrow_whenTheBodyHasNoFlag() {
        // Un cuerpo sin el campo no puede leerse como "no es el dueño": sería un 403 inventado.
        nextResponseBody = "{}";

        assertThatThrownBy(() -> adapter.isOwner(UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(ExternalServiceUnavailableException.class);
    }

    @Test
    void isOwner_shouldThrowExternalServiceUnavailable_whenPetsFails() {
        nextStatus = 500;

        assertThatThrownBy(() -> adapter.isOwner(UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(ExternalServiceUnavailableException.class);
    }

    @Test
    void getPets_shouldPostTheIds_andIndexTheResponseById() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        nextResponseBody = """
                [
                  {"id":"%s","ownerId":"%s","name":"Max","species":"DOG","breed":"Criollo",
                   "sex":"MALE","birthDate":"2021-05-10","photoUrl":"https://cdn/max.png",
                   "createdByVeterinaryId":null,"claimStatus":"CLAIMED"},
                  {"id":"%s","ownerId":"%s","name":"Luna","species":"CAT","breed":null,
                   "sex":"FEMALE","birthDate":null,"photoUrl":null,
                   "createdByVeterinaryId":null,"claimStatus":"CLAIMED"}
                ]
                """.formatted(first, ownerId, second, ownerId);

        Map<UUID, PetsServicePort.PetInfo> pets = adapter.getPets(List.of(first, second));

        RecordedRequest request = requests.poll();
        assertThat(request.method()).isEqualTo("POST");
        assertThat(request.path()).isEqualTo("/internal/pets/batch");
        assertThat(request.body()).contains(first.toString()).contains(second.toString())
                .startsWith("{\"ids\":[");

        assertThat(pets).hasSize(2);
        assertThat(pets.get(first).name()).isEqualTo("Max");
        assertThat(pets.get(first).ownerId()).isEqualTo(ownerId);
        assertThat(pets.get(first).birthDate()).isEqualTo(LocalDate.of(2021, 5, 10));
        assertThat(pets.get(second).species()).isEqualTo("CAT");
        assertThat(pets.get(second).birthDate()).isNull();
    }

    @Test
    void getPets_shouldIgnoreExtraFieldsPetsMayReturn() {
        UUID petId = UUID.randomUUID();
        // ownerContact no está en PetInfo (su forma se acordará en la Fase 4B): Jackson la ignora.
        nextResponseBody = """
                [{"id":"%s","ownerId":null,"name":"Max","species":"DOG","claimStatus":"UNCLAIMED",
                  "ownerContact":{"fullName":"Carlos","phone":"+573001234567"}}]
                """.formatted(petId);

        Map<UUID, PetsServicePort.PetInfo> pets = adapter.getPets(List.of(petId));

        assertThat(pets.get(petId).name()).isEqualTo("Max");
        assertThat(pets.get(petId).ownerId()).isNull();
    }

    @Test
    void getPets_shouldOmitIdsPetsDoesNotKnow() {
        UUID known = UUID.randomUUID();
        UUID unknown = UUID.randomUUID();
        nextResponseBody = """
                [{"id":"%s","ownerId":"%s","name":"Max","species":"DOG"}]
                """.formatted(known, UUID.randomUUID());

        Map<UUID, PetsServicePort.PetInfo> pets = adapter.getPets(List.of(known, unknown));

        assertThat(pets).containsOnlyKeys(known);
    }

    @Test
    void getPets_shouldNotCallPets_whenThereAreNoIds() {
        assertThat(adapter.getPets(List.of())).isEmpty();
        assertThat(adapter.getPets(null)).isEmpty();

        assertThat(requests).isEmpty();
    }

    @Test
    void getPets_shouldSendEachIdOnce() {
        UUID petId = UUID.randomUUID();
        nextResponseBody = """
                [{"id":"%s","ownerId":"%s","name":"Max","species":"DOG"}]
                """.formatted(petId, UUID.randomUUID());

        adapter.getPets(List.of(petId, petId, petId));

        RecordedRequest request = requests.poll();
        assertThat(request.body()).isEqualTo("{\"ids\":[\"" + petId + "\"]}");
    }

    @Test
    void getPets_shouldThrowExternalServiceUnavailable_whenPetsFails() {
        nextStatus = 503;

        assertThatThrownBy(() -> adapter.getPets(List.of(UUID.randomUUID())))
                .isInstanceOf(ExternalServiceUnavailableException.class);
    }
}

package com.MyAnimaLog.Veterinary.infrastructure.messaging;

import com.MyAnimaLog.Veterinary.application.shared.dto.DomainEvent;
import com.MyAnimaLog.Veterinary.application.shared.dto.EventMetadata;
import com.MyAnimaLog.Veterinary.domain.shared.enums.VeterinaryEventType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GooglePubSubEventAdapterTest {

    @Mock
    private PubSubTemplate pubSubTemplate;

    private GooglePubSubEventAdapter adapter;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        adapter = new GooglePubSubEventAdapter(pubSubTemplate, objectMapper);
    }

    @Test
    void publish_shouldSendToCorrectTopicWithEnvelopeFields() {
        UUID veterinaryId = UUID.randomUUID();
        DomainEvent event = DomainEvent.create(
                VeterinaryEventType.VETERINARY_REGISTERED,
                UUID.randomUUID(),
                null,
                Map.of("veterinaryId", veterinaryId.toString(), "name", "Clinica Patitas"),
                EventMetadata.of(veterinaryId, UUID.randomUUID())
        );
        when(pubSubTemplate.publish(anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture("message-id"));

        adapter.publish(event);

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(pubSubTemplate).publish(topicCaptor.capture(), payloadCaptor.capture());

        assertThat(topicCaptor.getValue()).isEqualTo("veterinary-clinic");
        assertThat(payloadCaptor.getValue())
                .contains("\"eventType\":\"VETERINARY_REGISTERED\"")
                .contains("\"occurredAt\"")
                .contains("\"metadata\"")
                .contains("\"version\":\"1.0\"")
                .contains("\"source\":\"veterinary-service\"");
    }

    @Test
    void publish_shouldNotPropagateException_whenTemplateThrowsSynchronously() {
        DomainEvent event = DomainEvent.create(
                VeterinaryEventType.USER_LINKED,
                UUID.randomUUID(),
                null,
                Map.of(),
                EventMetadata.of(UUID.randomUUID(), UUID.randomUUID())
        );
        when(pubSubTemplate.publish(anyString(), anyString()))
                .thenThrow(new RuntimeException("Pub/Sub unavailable"));

        assertThatCode(() -> adapter.publish(event)).doesNotThrowAnyException();
    }

    @Test
    void publish_shouldNotPropagateException_whenFutureCompletesExceptionally() {
        DomainEvent event = DomainEvent.create(
                VeterinaryEventType.USER_LINKED,
                UUID.randomUUID(),
                null,
                Map.of(),
                EventMetadata.of(UUID.randomUUID(), UUID.randomUUID())
        );
        CompletableFuture<String> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Broker timeout"));
        when(pubSubTemplate.publish(anyString(), anyString())).thenReturn(failedFuture);

        assertThatCode(() -> adapter.publish(event)).doesNotThrowAnyException();
    }
}

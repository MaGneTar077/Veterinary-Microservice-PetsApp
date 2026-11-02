package com.MyAnimaLog.Veterinary.infrastructure.messaging;

import com.MyAnimaLog.Veterinary.application.shared.dto.DomainEvent;
import com.MyAnimaLog.Veterinary.application.shared.dto.EventMetadata;
import com.MyAnimaLog.Veterinary.domain.shared.enums.VeterinaryEventType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
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

    private DomainEvent sampleEvent() {
        return DomainEvent.create(
                VeterinaryEventType.VETERINARY_SUSPENDED,
                UUID.randomUUID(),
                null,
                Map.of("veterinaryName", "Clinica Test"),
                EventMetadata.of(UUID.randomUUID(), UUID.randomUUID()));
    }

    @Test
    void publish_shouldDeferUntilAfterCommit_whenATransactionIsActive() {
        when(pubSubTemplate.publish(anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture("message-id"));
        DomainEvent event = sampleEvent();

        TransactionSynchronizationManager.initSynchronization();
        try {
            adapter.publish(event);

            verify(pubSubTemplate, never()).publish(anyString(), anyString());

            List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
            assertThat(synchronizations).hasSize(1);
            synchronizations.forEach(TransactionSynchronization::afterCommit);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        verify(pubSubTemplate, times(1)).publish(anyString(), anyString());
    }

    @Test
    void publish_shouldNeverSend_whenTheTransactionRollsBack() {
        DomainEvent event = sampleEvent();

        TransactionSynchronizationManager.initSynchronization();
        try {
            adapter.publish(event);
            // Simulates a rollback: the transaction manager would call afterCompletion(ROLLED_BACK),
            // never afterCommit() — so we just discard the registered synchronization without invoking it.
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        verify(pubSubTemplate, never()).publish(anyString(), anyString());
    }

    @Test
    void publish_shouldSendImmediately_whenNoTransactionIsActive() {
        when(pubSubTemplate.publish(anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture("message-id"));
        DomainEvent event = sampleEvent();

        assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

        adapter.publish(event);

        verify(pubSubTemplate, times(1)).publish(anyString(), anyString());
    }

    @Test
    void publish_shouldLogAndNotThrow_whenPublishingAfterCommitFails() {
        when(pubSubTemplate.publish(anyString(), anyString()))
                .thenThrow(new RuntimeException("Pub/Sub unavailable"));
        DomainEvent event = sampleEvent();

        TransactionSynchronizationManager.initSynchronization();
        try {
            adapter.publish(event);
            List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();

            assertThatCode(() -> synchronizations.forEach(TransactionSynchronization::afterCommit))
                    .doesNotThrowAnyException();
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}

package com.MyAnimaLog.Veterinary.infrastructure.messaging;

import com.MyAnimaLog.Veterinary.domain.shared.enums.VeterinaryEventType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class VeterinaryEventTypeTopicCoverageTest {

    @Mock
    private PubSubTemplate pubSubTemplate;

    @Test
    void everyEventTypeMustHaveATopicConfigured() {
        GooglePubSubEventAdapter adapter = new GooglePubSubEventAdapter(pubSubTemplate, new ObjectMapper());

        for (VeterinaryEventType eventType : VeterinaryEventType.values()) {
            assertThat(adapter.topicFor(eventType))
                    .as("VeterinaryEventType.%s has no Pub/Sub topic mapped in GooglePubSubEventAdapter", eventType)
                    .isNotNull();
        }
    }
}

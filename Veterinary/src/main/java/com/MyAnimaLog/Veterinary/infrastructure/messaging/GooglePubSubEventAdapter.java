package com.MyAnimaLog.Veterinary.infrastructure.messaging;

import com.MyAnimaLog.Veterinary.application.shared.dto.DomainEvent;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.EventPublisherPort;
import com.MyAnimaLog.Veterinary.domain.shared.enums.VeterinaryEventType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.events.enabled", havingValue = "true", matchIfMissing = true)
public class GooglePubSubEventAdapter implements EventPublisherPort {

    private final PubSubTemplate pubSubTemplate;
    private final ObjectMapper objectMapper;

    private static final Map<VeterinaryEventType, String> TOPICS = new EnumMap<>(VeterinaryEventType.class);

    static {
        TOPICS.put(VeterinaryEventType.VETERINARY_REGISTERED, "veterinary-clinic");
        TOPICS.put(VeterinaryEventType.VETERINARY_SUSPENDED, "veterinary-clinic");
        TOPICS.put(VeterinaryEventType.VETERINARY_REACTIVATED, "veterinary-clinic");

        TOPICS.put(VeterinaryEventType.VERIFICATION_SUBMITTED, "veterinary-verification");
        TOPICS.put(VeterinaryEventType.VERIFICATION_APPROVED, "veterinary-verification");
        TOPICS.put(VeterinaryEventType.VERIFICATION_REJECTED, "veterinary-verification");
        TOPICS.put(VeterinaryEventType.VERIFICATION_CORRECTION_REQUESTED, "veterinary-verification");

        TOPICS.put(VeterinaryEventType.EMPLOYEE_INVITED, "veterinary-staff");
        TOPICS.put(VeterinaryEventType.EMPLOYEE_JOINED, "veterinary-staff");
        TOPICS.put(VeterinaryEventType.EMPLOYEE_ROLE_UPDATED, "veterinary-staff");
        TOPICS.put(VeterinaryEventType.EMPLOYEE_DEACTIVATED, "veterinary-staff");
        TOPICS.put(VeterinaryEventType.OWNERSHIP_TRANSFERRED, "veterinary-staff");

        TOPICS.put(VeterinaryEventType.USER_LINKED, "veterinary-patients");
        TOPICS.put(VeterinaryEventType.USER_UNLINKED, "veterinary-patients");
        TOPICS.put(VeterinaryEventType.PET_SHARED_WITH_VETERINARY, "veterinary-patients");
        TOPICS.put(VeterinaryEventType.PET_SHARE_REVOKED, "veterinary-patients");

        TOPICS.put(VeterinaryEventType.SUBSCRIPTION_TRIAL_STARTED, "veterinary-subscription");
        TOPICS.put(VeterinaryEventType.SUBSCRIPTION_EXPIRING, "veterinary-subscription");
        TOPICS.put(VeterinaryEventType.SUBSCRIPTION_IN_GRACE, "veterinary-subscription");
        TOPICS.put(VeterinaryEventType.SUBSCRIPTION_EXPIRED, "veterinary-subscription");
        TOPICS.put(VeterinaryEventType.SUBSCRIPTION_RENEWED, "veterinary-subscription");
        TOPICS.put(VeterinaryEventType.PAYMENT_FAILED, "veterinary-subscription");
    }

    public String topicFor(VeterinaryEventType eventType) {
        return TOPICS.get(eventType);
    }

    @Override
    public void publish(DomainEvent event) {
        UUID veterinaryId = event.metadata() != null ? event.metadata().veterinaryId() : null;
        String topic = topicFor(event.eventType());
        if (topic == null) {
            log.error("No Pub/Sub topic configured for eventType={}, veterinaryId={}", event.eventType(), veterinaryId);
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(event);
            pubSubTemplate.publish(topic, json)
                    .exceptionally(ex -> {
                        log.error("Failed to publish eventType={} veterinaryId={} to topic={}",
                                event.eventType(), veterinaryId, topic, ex);
                        return null;
                    });
        } catch (Exception ex) {
            log.error("Failed to publish eventType={} veterinaryId={} to topic={}",
                    event.eventType(), veterinaryId, topic, ex);
        }
    }
}

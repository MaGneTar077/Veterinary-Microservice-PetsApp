package com.MyAnimaLog.Veterinary.infrastructure.messaging;

import com.MyAnimaLog.Veterinary.application.shared.dto.DomainEvent;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.EventPublisherPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.events.enabled", havingValue = "false")
public class NoOpEventPublisher implements EventPublisherPort {

    @Override
    public void publish(DomainEvent event) {
        log.debug("app.events.enabled=false; dropping eventType={}", event.eventType());
    }
}

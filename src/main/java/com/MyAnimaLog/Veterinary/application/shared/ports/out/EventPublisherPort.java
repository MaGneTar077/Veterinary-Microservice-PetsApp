package com.MyAnimaLog.Veterinary.application.shared.ports.out;

import com.MyAnimaLog.Veterinary.application.shared.dto.DomainEvent;

public interface EventPublisherPort {
    void publish(DomainEvent event);
}

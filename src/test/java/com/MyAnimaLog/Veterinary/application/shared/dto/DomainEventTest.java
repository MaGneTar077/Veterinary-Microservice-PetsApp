package com.MyAnimaLog.Veterinary.application.shared.dto;

import com.MyAnimaLog.Veterinary.domain.shared.enums.VeterinaryEventType;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DomainEventTest {

    @Test
    void create_shouldThrow_whenUserIdAndRecipientAreBothNull() {
        assertThatThrownBy(() -> DomainEvent.create(
                VeterinaryEventType.USER_LINKED,
                null,
                null,
                Map.of(),
                EventMetadata.of(UUID.randomUUID(), UUID.randomUUID())
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void create_shouldSucceed_whenOnlyUserIdIsPresent() {
        DomainEvent event = DomainEvent.create(
                VeterinaryEventType.USER_LINKED,
                UUID.randomUUID(),
                null,
                Map.of(),
                EventMetadata.of(UUID.randomUUID(), UUID.randomUUID())
        );

        assertThat(event.occurredAt()).isNotNull();
        assertThat(event.recipient()).isNull();
    }

    @Test
    void create_shouldSucceed_whenOnlyRecipientIsPresent() {
        DomainEvent event = DomainEvent.create(
                VeterinaryEventType.EMPLOYEE_INVITED,
                null,
                new Recipient("owner@example.com", null, "Ana"),
                Map.of(),
                EventMetadata.of(UUID.randomUUID(), UUID.randomUUID())
        );

        assertThat(event.occurredAt()).isNotNull();
        assertThat(event.userId()).isNull();
    }
}

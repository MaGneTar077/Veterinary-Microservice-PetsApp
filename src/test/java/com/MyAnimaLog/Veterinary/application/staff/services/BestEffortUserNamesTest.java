package com.MyAnimaLog.Veterinary.application.staff.services;

import com.MyAnimaLog.Veterinary.application.shared.ports.out.UserServicePort;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.ExternalServiceUnavailableException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BestEffortUserNamesTest {

    @Test
    void resolve_shouldReturnEmptyMap_forAnEmptyList() {
        UserServicePort userServicePort = mock(UserServicePort.class);

        assertThat(BestEffortUserNames.resolve(userServicePort, List.of(), Duration.ofMillis(500))).isEmpty();
        verifyNoInteractions(userServicePort);
    }

    @Test
    void resolve_shouldResolveEveryIdInParallel_whenAllAnswerQuickly() {
        UserServicePort userServicePort = mock(UserServicePort.class);
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        when(userServicePort.getUser(a)).thenReturn(new UserServicePort.UserInfo(a, "a@x.com", true, "Ana"));
        when(userServicePort.getUser(b)).thenReturn(new UserServicePort.UserInfo(b, "b@x.com", true, "Beto"));

        Map<UUID, UserServicePort.UserInfo> result =
                BestEffortUserNames.resolve(userServicePort, List.of(a, b), Duration.ofMillis(1500));

        assertThat(result.get(a).name()).isEqualTo("Ana");
        assertThat(result.get(b).name()).isEqualTo("Beto");
    }

    @Test
    void resolve_shouldDeduplicateIds_callingTheUserServiceOncePerDistinctId() {
        UserServicePort userServicePort = mock(UserServicePort.class);
        UUID a = UUID.randomUUID();
        when(userServicePort.getUser(a)).thenReturn(new UserServicePort.UserInfo(a, "a@x.com", true, "Ana"));

        BestEffortUserNames.resolve(userServicePort, List.of(a, a, a), Duration.ofMillis(500));

        verify(userServicePort, times(1)).getUser(a);
    }

    @Test
    void resolve_shouldReturnNull_whenTheUserServiceThrows() {
        UserServicePort userServicePort = mock(UserServicePort.class);
        UUID a = UUID.randomUUID();
        when(userServicePort.getUser(a)).thenThrow(new ExternalServiceUnavailableException("down"));

        Map<UUID, UserServicePort.UserInfo> result =
                BestEffortUserNames.resolve(userServicePort, List.of(a), Duration.ofMillis(500));

        assertThat(result).containsEntry(a, null);
    }

    /**
     * Si una llamada se cuelga más allá del presupuesto, esa entrada queda {@code null} — pero el
     * presupuesto es <strong>total</strong>, no por id: con dos ids lentos en paralelo, los dos
     * terminan sin null si responden antes de que se agote.
     */
    @Test
    void resolve_shouldLeavePendingIdsAsNull_whenTheBudgetRunsOut() {
        UserServicePort userServicePort = mock(UserServicePort.class);
        UUID fast = UUID.randomUUID();
        UUID slow = UUID.randomUUID();
        when(userServicePort.getUser(fast)).thenReturn(new UserServicePort.UserInfo(fast, "f@x.com", true, "Rápido"));
        when(userServicePort.getUser(slow)).thenAnswer(invocation -> {
            Thread.sleep(3000);
            return new UserServicePort.UserInfo(slow, "s@x.com", true, "Lento");
        });

        Map<UUID, UserServicePort.UserInfo> result =
                BestEffortUserNames.resolve(userServicePort, List.of(fast, slow), Duration.ofMillis(300));

        assertThat(result.get(fast).name()).isEqualTo("Rápido");
        assertThat(result.get(slow)).isNull();
    }

    @Test
    void resolve_shouldRunCallsConcurrently_notSequentially() {
        UserServicePort userServicePort = mock(UserServicePort.class);
        AtomicInteger concurrent = new AtomicInteger(0);
        AtomicInteger maxConcurrent = new AtomicInteger(0);
        List<UUID> ids = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        when(userServicePort.getUser(any())).thenAnswer(invocation -> {
            int now = concurrent.incrementAndGet();
            maxConcurrent.updateAndGet(max -> Math.max(max, now));
            Thread.sleep(200);
            concurrent.decrementAndGet();
            UUID id = invocation.getArgument(0);
            return new UserServicePort.UserInfo(id, "x@x.com", true, "X");
        });

        BestEffortUserNames.resolve(userServicePort, ids, Duration.ofMillis(1500));

        assertThat(maxConcurrent.get()).isGreaterThan(1);
    }
}

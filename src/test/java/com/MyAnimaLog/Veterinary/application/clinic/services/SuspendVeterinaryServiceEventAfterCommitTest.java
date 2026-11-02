package com.MyAnimaLog.Veterinary.application.clinic.services;

import com.MyAnimaLog.Veterinary.application.clinic.dto.SuspendVeterinaryRequest;
import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.dto.AuthenticatedUser;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.AuthenticatedUserPort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.domain.staff.model.VeterinaryEmployee;
import com.MyAnimaLog.Veterinary.infrastructure.messaging.GooglePubSubEventAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Exercises the real {@link SuspendVeterinaryService} wired to the real {@link GooglePubSubEventAdapter}
 * (with a mocked {@link PubSubTemplate}, so no GCP credentials are needed) — confirms the combination
 * actually defers the Pub/Sub send to {@code afterCommit}, not just the adapter in isolation.
 */
@ExtendWith(MockitoExtension.class)
class SuspendVeterinaryServiceEventAfterCommitTest {

    @Mock
    private VeterinaryRepositoryPort veterinaryRepositoryPort;
    @Mock
    private VeterinaryEmployeeRepositoryPort employeeRepositoryPort;
    @Mock
    private VeterinaryAuthorizationService authorizationService;
    @Mock
    private AuthenticatedUserPort authenticatedUserPort;
    @Mock
    private PubSubTemplate pubSubTemplate;

    private SuspendVeterinaryService service;
    private UUID veterinaryId;
    private UUID adminUserId;
    private UUID ownerUserId;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();
        adminUserId = UUID.randomUUID();
        ownerUserId = UUID.randomUUID();

        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        GooglePubSubEventAdapter realAdapter = new GooglePubSubEventAdapter(pubSubTemplate, objectMapper);

        service = new SuspendVeterinaryService(
                veterinaryRepositoryPort, employeeRepositoryPort, authorizationService, authenticatedUserPort, realAdapter);
        ReflectionTestUtils.setField(service, "supportEmail", "soporte@myanimalog.com");

        Veterinary activeVeterinary = Veterinary.builder()
                .id(veterinaryId).name("Clínica El Bosque").status(VeterinaryStatus.ACTIVE).active(true).build();
        VeterinaryEmployee owner = VeterinaryEmployee.builder()
                .id(UUID.randomUUID()).veterinaryId(veterinaryId).userId(ownerUserId).role(EmployeeRole.OWNER).active(true).build();

        lenient().when(authenticatedUserPort.current())
                .thenReturn(new AuthenticatedUser(adminUserId, "admin@example.com", true, true, Optional.empty()));
        lenient().when(employeeRepositoryPort.existsByVeterinaryIdAndUserId(veterinaryId, adminUserId)).thenReturn(false);
        lenient().when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(activeVeterinary));
        lenient().when(veterinaryRepositoryPort.claimStatusTransition(veterinaryId, VeterinaryStatus.ACTIVE, VeterinaryStatus.SUSPENDED))
                .thenReturn(true);
        lenient().when(veterinaryRepositoryPort.save(any(Veterinary.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(employeeRepositoryPort.findByVeterinaryIdAndRoleAndActiveTrue(veterinaryId, EmployeeRole.OWNER))
                .thenReturn(Optional.of(owner));
        lenient().when(pubSubTemplate.publish(anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture("message-id"));
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void suspend_shouldPublishToPubSub_onlyAfterTheSurroundingTransactionCommits() {
        TransactionSynchronizationManager.initSynchronization();
        try {
            service.suspend(veterinaryId, SuspendVeterinaryRequest.builder()
                    .reason("Pago atrasado por mas de 60 dias").build());

            verify(pubSubTemplate, never()).publish(anyString(), anyString());

            List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
            synchronizations.forEach(TransactionSynchronization::afterCommit);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        verify(pubSubTemplate, times(1)).publish(anyString(), anyString());
    }

    @Test
    void suspend_shouldNeverReachPubSub_whenSomethingFailsAfterPublishAndTheTransactionRollsBack() {
        TransactionSynchronizationManager.initSynchronization();
        try {
            service.suspend(veterinaryId, SuspendVeterinaryRequest.builder()
                    .reason("Pago atrasado por mas de 60 dias").build());

            // Simulates a failure elsewhere in the same transaction, after suspend() already
            // registered its synchronization: the transaction manager rolls back and never calls
            // afterCommit() on it.
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        verify(pubSubTemplate, never()).publish(anyString(), anyString());
    }
}

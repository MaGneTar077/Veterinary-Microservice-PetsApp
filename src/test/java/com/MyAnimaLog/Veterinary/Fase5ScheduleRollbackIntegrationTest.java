package com.MyAnimaLog.Veterinary;

import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.schedule.dto.ScheduleEntryRequest;
import com.MyAnimaLog.Veterinary.application.schedule.ports.in.ReplaceBusinessHoursUseCase;
import com.MyAnimaLog.Veterinary.application.schedule.ports.out.BusinessHoursRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.domain.schedule.model.BusinessHours;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;

/**
 * Rollback <strong>real</strong> del reemplazo de la semana: a diferencia del resto de los tests de
 * persistencia, este <strong>no</strong> es {@code @Transactional}, porque necesita que el estado
 * previo esté realmente confirmado para poder comprobar que sigue ahí después de que el reemplazo
 * falle. Por eso crea su propia clínica temporal y la borra en un {@code finally} con SQL nativo
 * ({@code VeterinaryRepositoryPort} no expone borrado), para no dejar basura en la base compartida.
 *
 * <p>El fallo se fuerza con un spy del puerto que lanza en {@code saveAll} después de que el
 * borrado ya ocurrió dentro de la transacción del caso de uso: si el método no fuera
 * {@code @Transactional}, la semana anterior se habría perdido y la clínica quedaría sin horario
 * (para Calendar, "no atiende nunca").
 */
@SpringBootTest
@ActiveProfiles("test")
class Fase5ScheduleRollbackIntegrationTest {

    @Autowired
    private ReplaceBusinessHoursUseCase replaceBusinessHoursUseCase;
    @Autowired
    private VeterinaryRepositoryPort veterinaryRepositoryPort;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private PlatformTransactionManager transactionManager;

    /** El caso de uso exige CLINIC_CONFIGURE; acá no se está probando la autorización. */
    @MockitoBean
    private VeterinaryAuthorizationService authorizationService;

    @MockitoSpyBean
    private BusinessHoursRepositoryPort businessHoursRepositoryPort;

    @Test
    void replaceBusinessHours_shouldKeepThePreviousWeek_whenTheInsertFails() {
        UUID veterinaryId = veterinaryRepositoryPort.save(TestClinics.build()).getId();
        try {
            businessHoursRepositoryPort.saveAll(List.of(BusinessHours.builder()
                    .id(UUID.randomUUID())
                    .veterinaryId(veterinaryId)
                    .dayOfWeek(1)
                    .opensAt(LocalTime.of(8, 0))
                    .closesAt(LocalTime.of(12, 0))
                    .build()));
            assertThat(businessHoursRepositoryPort.findByVeterinaryId(veterinaryId)).hasSize(1);

            doThrow(new IllegalStateException("insert failed")).when(businessHoursRepositoryPort)
                    .saveAll(anyList());

            assertThatThrownBy(() -> replaceBusinessHoursUseCase.replace(veterinaryId, List.of(
                    ScheduleEntryRequest.builder().dayOfWeek(5).opensAt("09:00").closesAt("13:00").build())))
                    .isInstanceOf(IllegalStateException.class);

            List<BusinessHours> afterFailure = businessHoursRepositoryPort.findByVeterinaryId(veterinaryId);

            assertThat(afterFailure).hasSize(1);
            assertThat(afterFailure.get(0).getDayOfWeek()).isEqualTo(1);
            assertThat(afterFailure.get(0).getOpensAt()).isEqualTo(LocalTime.of(8, 0));
        } finally {
            cleanUp(veterinaryId);
        }
    }

    private void cleanUp(UUID veterinaryId) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            entityManager.createNativeQuery("DELETE FROM public.business_hours WHERE veterinary_id = :id")
                    .setParameter("id", veterinaryId)
                    .executeUpdate();
            entityManager.createNativeQuery("DELETE FROM public.veterinary WHERE id = :id")
                    .setParameter("id", veterinaryId)
                    .executeUpdate();
        });
    }
}

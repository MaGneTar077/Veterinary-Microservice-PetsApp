package com.MyAnimaLog.Veterinary;

import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.subscription.ports.out.VeterinarySubscriptionRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Nit;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.subscription.model.VeterinarySubscription;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reproduces and verifies the fix for the 500 on {@code GET /api/veterinary/admin/veterinaries}:
 * {@code VeterinaryJpaRepository.findForAdminList} used to be a JPQL query with
 * {@code (:param IS NULL OR ...)} guards for every optional filter. Hibernate 6 can't always infer
 * a JDBC type for a parameter whose only usage is "compare to NULL" (confirmed against the real
 * database: PostgreSQL rejected the untyped bind with "function lower(bytea) does not exist" when
 * every filter was absent). {@code VeterinaryAdminSpecifications} now builds the WHERE clause one
 * predicate at a time, adding a condition only for filters that are actually present — these tests
 * hit the real Supabase Postgres instance through {@code VeterinaryRepositoryPort.findForAdminList}
 * exactly as the admin "Clinicas" screen does, with every combination of filters.
 *
 * <p>{@code @Transactional} reverts everything at the end of each test. Every clinic uses a unique
 * marker (city/department/name) so assertions never depend on whatever else happens to already be
 * in the shared database.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminClinicsListingFilterPersistenceIntegrationTest {

    @Autowired
    private VeterinaryRepositoryPort veterinaryRepositoryPort;

    @Autowired
    private VeterinarySubscriptionRepositoryPort subscriptionRepositoryPort;

    private Veterinary persistClinic(String marker, VeterinaryStatus status) {
        return veterinaryRepositoryPort.save(Veterinary.builder()
                .id(UUID.randomUUID())
                .name("Clinica " + marker)
                .legalName("Clinica " + marker + " SAS")
                .nit(Nit.fromPersisted(UUID.randomUUID().toString().substring(0, 18)))
                .city("Ciudad-" + marker)
                .department("Depto-" + marker)
                .email(marker.toLowerCase() + "@example.com")
                .tenantId(UUID.randomUUID().toString())
                .status(status)
                .active(status.impliesActiveFlag())
                .timezone("America/Bogota")
                .currency("COP")
                .defaultAppointmentMinutes(30)
                .allowOnlineBooking(true)
                .bookingRequiresConfirmation(true)
                .cancellationMinHours(4)
                .directoryVisible(true)
                .createdAt(LocalDateTime.now())
                .build());
    }

    private void persistActiveSubscription(UUID veterinaryId, String plan) {
        subscriptionRepositoryPort.save(VeterinarySubscription.builder()
                .id(UUID.randomUUID())
                .veterinaryId(veterinaryId)
                .plan(plan)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(30))
                .active(true)
                .build());
    }

    @Test
    void findForAdminList_shouldNotFail_whenNoFiltersAreProvided() {
        String marker = unique("NOFILTER");
        persistClinic(marker, VeterinaryStatus.ACTIVE);

        assertThatCode(() -> veterinaryRepositoryPort.findForAdminList(null, null, null, null, null, 0, 50))
                .doesNotThrowAnyException();
    }

    @Test
    void findForAdminList_shouldFilterByStatusOnly() {
        String marker = unique("STATUS");
        Veterinary suspended = persistClinic(marker, VeterinaryStatus.SUSPENDED);
        Veterinary active = persistClinic(marker + "-other", VeterinaryStatus.ACTIVE);

        VeterinaryRepositoryPort.AdminClinicPage page =
                veterinaryRepositoryPort.findForAdminList(VeterinaryStatus.SUSPENDED, null, null, null, null, 0, 5000);

        assertThat(page.items()).extracting(Veterinary::getId).contains(suspended.getId());
        assertThat(page.items()).extracting(Veterinary::getId).doesNotContain(active.getId());
        assertThat(page.items()).allSatisfy(item -> assertThat(item.getStatus()).isEqualTo(VeterinaryStatus.SUSPENDED));
    }

    @Test
    void findForAdminList_shouldFilterBySearchOnly() {
        String marker = unique("SEARCH");
        Veterinary clinic = persistClinic(marker, VeterinaryStatus.ACTIVE);

        VeterinaryRepositoryPort.AdminClinicPage page =
                veterinaryRepositoryPort.findForAdminList(null, "clinica " + marker.toLowerCase(), null, null, null, 0, 50);

        assertThat(page.items()).extracting(Veterinary::getId).containsExactly(clinic.getId());
    }

    @Test
    void findForAdminList_shouldFilterByCityOnly() {
        String marker = unique("CITY");
        Veterinary clinic = persistClinic(marker, VeterinaryStatus.ACTIVE);

        VeterinaryRepositoryPort.AdminClinicPage page =
                veterinaryRepositoryPort.findForAdminList(null, null, null, "ciudad-" + marker.toLowerCase(), null, 0, 50);

        assertThat(page.items()).extracting(Veterinary::getId).containsExactly(clinic.getId());
    }

    @Test
    void findForAdminList_shouldFilterByDepartmentOnly() {
        String marker = unique("DEPT");
        Veterinary clinic = persistClinic(marker, VeterinaryStatus.ACTIVE);

        VeterinaryRepositoryPort.AdminClinicPage page =
                veterinaryRepositoryPort.findForAdminList(null, null, null, null, "depto-" + marker.toLowerCase(), 0, 50);

        assertThat(page.items()).extracting(Veterinary::getId).containsExactly(clinic.getId());
    }

    @Test
    void findForAdminList_shouldFilterByPlanOnly() {
        String marker = unique("PLAN");
        Veterinary withPlan = persistClinic(marker, VeterinaryStatus.ACTIVE);
        persistActiveSubscription(withPlan.getId(), "PRO_" + marker);
        persistClinic(marker + "-noplan", VeterinaryStatus.ACTIVE);

        VeterinaryRepositoryPort.AdminClinicPage page =
                veterinaryRepositoryPort.findForAdminList(null, null, "PRO_" + marker, null, null, 0, 50);

        assertThat(page.items()).extracting(Veterinary::getId).containsExactly(withPlan.getId());
    }

    @Test
    void findForAdminList_shouldFilterByAllFiltersCombined() {
        String marker = unique("ALL");
        Veterinary clinic = persistClinic(marker, VeterinaryStatus.ACTIVE);
        persistActiveSubscription(clinic.getId(), "PRO_" + marker);

        VeterinaryRepositoryPort.AdminClinicPage page = veterinaryRepositoryPort.findForAdminList(
                VeterinaryStatus.ACTIVE, "clinica " + marker.toLowerCase(), "PRO_" + marker,
                "ciudad-" + marker.toLowerCase(), "depto-" + marker.toLowerCase(), 0, 50);

        assertThat(page.items()).extracting(Veterinary::getId).containsExactly(clinic.getId());
    }

    @Test
    void findForAdminList_shouldTreatBlankSearchAndBlankCityAsAbsent() {
        String marker = unique("BLANK");
        Veterinary clinic = persistClinic(marker, VeterinaryStatus.ACTIVE);

        assertThatCode(() -> veterinaryRepositoryPort.findForAdminList(null, "   ", null, "", null, 0, 50))
                .doesNotThrowAnyException();

        // A blank search/city must behave exactly like "absent" -- narrow down with a real filter
        // instead so the assertion stays deterministic regardless of what else is in the database.
        VeterinaryRepositoryPort.AdminClinicPage page =
                veterinaryRepositoryPort.findForAdminList(null, "   ", null, "", "depto-" + marker.toLowerCase(), 0, 50);
        assertThat(page.items()).extracting(Veterinary::getId).containsExactly(clinic.getId());
    }

    private static String unique(String prefix) {
        return prefix + UUID.randomUUID().toString().substring(0, 8);
    }
}

package com.MyAnimaLog.Veterinary;

import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.patients.ports.out.UserVeterinaryLinkRepositoryPort;
import com.MyAnimaLog.Veterinary.application.patients.ports.out.VeterinaryPatientRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.patients.enums.ConsentStatus;
import com.MyAnimaLog.Veterinary.domain.patients.enums.LinkStatus;
import com.MyAnimaLog.Veterinary.domain.patients.enums.PatientOrigin;
import com.MyAnimaLog.Veterinary.domain.patients.model.UserVeterinaryLink;
import com.MyAnimaLog.Veterinary.domain.patients.model.VeterinaryPatient;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Equivalente de {@code Fase3StaffTablesPersistenceIntegrationTest} para {@code 008_patients.sql}:
 * guarda por el camino real (Port → Mapper → Entity → Hibernate → Supabase) con solo los campos que
 * setea un servicio, para detectar lo que los tests con Mockito no pueden ver —una columna mal
 * mapeada, o una {@code NOT NULL DEFAULT} que Hibernate pisa con {@code null} porque el modelo de
 * dominio no tiene {@code @Builder.Default}.
 *
 * <p>Cada caso <strong>termina leyendo por el puerto</strong>: {@code save(...)} sobre una entidad
 * con id asignado no hace flush por sí solo y {@code @Transactional} revierte antes del commit, así
 * que un test que solo guarda pasa en verde sin haber ejecutado el {@code INSERT} (ver CLAUDE.md).
 * Por lo mismo, las filas con FK a {@code public.veterinary} usan una clínica real de
 * {@link TestClinics}.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class Fase4APatientsPersistenceIntegrationTest {

    @Autowired
    private VeterinaryRepositoryPort veterinaryRepositoryPort;

    @Autowired
    private UserVeterinaryLinkRepositoryPort linkRepositoryPort;

    @Autowired
    private VeterinaryPatientRepositoryPort patientRepositoryPort;

    @Autowired
    private EntityManager entityManager;

    private UUID persistClinic() {
        return TestClinics.persist(veterinaryRepositoryPort);
    }

    // ------------------------------------------------------------------
    // user_veterinary_link
    // ------------------------------------------------------------------

    @Test
    void link_shouldPersist_withOnlyTheFieldsLinkToVeterinarySets() {
        UUID veterinaryId = persistClinic();
        UUID userId = UUID.randomUUID();

        linkRepositoryPort.save(UserVeterinaryLink.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .veterinaryId(veterinaryId)
                // status se omite a propósito: tiene que salir del @Builder.Default del modelo,
                // no del DEFAULT de la columna (Hibernate insertaría NULL explícito).
                .linkedAt(LocalDateTime.now())
                .ownerName("Ana Pérez")
                .ownerEmail("ana@correo.com")
                .build());

        UserVeterinaryLink saved = linkRepositoryPort.findActiveLink(userId, veterinaryId).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(LinkStatus.LINKED);
        assertThat(saved.getOwnerName()).isEqualTo("Ana Pérez");
        assertThat(saved.getOwnerEmail()).isEqualTo("ana@correo.com");
        assertThat(saved.getOwnerPhone()).isNull();
        assertThat(saved.getUnlinkedAt()).isNull();
    }

    @Test
    void link_findActiveLink_shouldIgnoreAnUnlinkedRow() {
        UUID veterinaryId = persistClinic();
        UUID userId = UUID.randomUUID();

        linkRepositoryPort.save(UserVeterinaryLink.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .veterinaryId(veterinaryId)
                .status(LinkStatus.UNLINKED)
                .linkedAt(LocalDateTime.now().minusMonths(2))
                .unlinkedAt(LocalDateTime.now().minusMonths(1))
                .ownerName("Ana Pérez")
                .build());
        entityManager.flush();

        assertThat(linkRepositoryPort.findActiveLink(userId, veterinaryId)).isEmpty();
        // Pero la fila sigue ahí: desvincularse no borra, y por eso el re-vincular la reactiva.
        assertThat(linkRepositoryPort.findLatestLink(userId, veterinaryId)).isPresent();
    }

    @Test
    void link_findLatestLink_shouldReturnTheMostRecentRowOfThePair() {
        UUID veterinaryId = persistClinic();
        UUID userId = UUID.randomUUID();
        UUID newestId = UUID.randomUUID();

        // Dos filas UNLINKED del mismo par: es exactamente lo que deja la deduplicación de
        // 008_patients.sql en datos viejos, y la razón por la que findLatestLink no puede ser
        // una consulta que devuelva Optional sobre (user_id, veterinary_id).
        linkRepositoryPort.save(UserVeterinaryLink.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .veterinaryId(veterinaryId)
                .status(LinkStatus.UNLINKED)
                .linkedAt(LocalDateTime.now().minusMonths(6))
                .unlinkedAt(LocalDateTime.now().minusMonths(5))
                .build());
        linkRepositoryPort.save(UserVeterinaryLink.builder()
                .id(newestId)
                .userId(userId)
                .veterinaryId(veterinaryId)
                .status(LinkStatus.UNLINKED)
                .linkedAt(LocalDateTime.now().minusMonths(2))
                .unlinkedAt(LocalDateTime.now().minusMonths(1))
                .build());
        entityManager.flush();

        assertThat(linkRepositoryPort.findLatestLink(userId, veterinaryId).orElseThrow().getId())
                .isEqualTo(newestId);
    }

    @Test
    void link_findActiveByUserId_shouldReturnOnlyLinkedRows() {
        UUID activeClinic = persistClinic();
        UUID unlinkedClinic = persistClinic();
        UUID userId = UUID.randomUUID();

        linkRepositoryPort.save(UserVeterinaryLink.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .veterinaryId(activeClinic)
                .linkedAt(LocalDateTime.now())
                .build());
        linkRepositoryPort.save(UserVeterinaryLink.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .veterinaryId(unlinkedClinic)
                .status(LinkStatus.UNLINKED)
                .linkedAt(LocalDateTime.now().minusMonths(1))
                .unlinkedAt(LocalDateTime.now())
                .build());
        entityManager.flush();

        assertThat(linkRepositoryPort.findActiveByUserId(userId))
                .extracting(UserVeterinaryLink::getVeterinaryId)
                .containsExactly(activeClinic);
    }

    // ------------------------------------------------------------------
    // veterinary_patients
    // ------------------------------------------------------------------

    @Test
    void patient_shouldPersist_withOnlyTheFieldsSharePetsSets() {
        UUID veterinaryId = persistClinic();
        UUID petId = UUID.randomUUID();

        patientRepositoryPort.save(VeterinaryPatient.builder()
                .id(UUID.randomUUID())
                .veterinaryId(veterinaryId)
                .petId(petId)
                // origin y consentStatus se omiten: salen del @Builder.Default del modelo.
                .grantedBy(UUID.randomUUID())
                .grantedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .petName("Max")
                .species("DOG")
                .build());

        VeterinaryPatient saved = patientRepositoryPort
                .findByVeterinaryIdAndPetId(veterinaryId, petId).orElseThrow();
        assertThat(saved.getOrigin()).isEqualTo(PatientOrigin.SHARED_BY_OWNER);
        assertThat(saved.getConsentStatus()).isEqualTo(ConsentStatus.GRANTED);
        assertThat(saved.getPetName()).isEqualTo("Max");
        assertThat(saved.getSpecies()).isEqualTo("DOG");
        assertThat(saved.getRevokedAt()).isNull();
        // updated_at es NOT NULL y no la setea nadie: la pone @UpdateTimestamp.
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    /**
     * {@code updated_at} la mantiene Hibernate, no los servicios. Este test es la red que impide
     * que alguien convierta la revocación en un {@code UPDATE} masivo de JPQL —que
     * <strong>no</strong> dispara {@code @UpdateTimestamp}— sin que nada falle.
     */
    @Test
    void patient_updatedAt_shouldChangeOnRevokeAndOnReactivate() throws InterruptedException {
        UUID veterinaryId = persistClinic();
        UUID petId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();

        patientRepositoryPort.save(VeterinaryPatient.builder()
                .id(patientId)
                .veterinaryId(veterinaryId)
                .petId(petId)
                .grantedBy(ownerId)
                .grantedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .petName("Max")
                .build());
        entityManager.flush();
        entityManager.clear();

        LocalDateTime afterInsert = patientRepositoryPort
                .findByVeterinaryIdAndPetId(veterinaryId, petId).orElseThrow().getUpdatedAt();
        assertThat(afterInsert).isNotNull();

        // El reloj de la base tiene microsegundos, pero una pausa mínima evita que dos escrituras
        // caigan en el mismo instante y el test no pruebe nada.
        Thread.sleep(10);

        VeterinaryPatient granted = patientRepositoryPort
                .findByVeterinaryIdAndPetId(veterinaryId, petId).orElseThrow();
        patientRepositoryPort.save(granted.toBuilder()
                .consentStatus(ConsentStatus.REVOKED)
                .revokedAt(LocalDateTime.now())
                .build());
        entityManager.flush();
        entityManager.clear();

        VeterinaryPatient revoked = patientRepositoryPort
                .findByVeterinaryIdAndPetId(veterinaryId, petId).orElseThrow();
        assertThat(revoked.getConsentStatus()).isEqualTo(ConsentStatus.REVOKED);
        assertThat(revoked.getUpdatedAt()).isAfter(afterInsert);
        assertThat(revoked.getCreatedAt()).isNotNull();

        Thread.sleep(10);

        patientRepositoryPort.save(revoked.toBuilder()
                .consentStatus(ConsentStatus.GRANTED)
                .grantedAt(LocalDateTime.now())
                .revokedAt(null)
                .build());
        entityManager.flush();
        entityManager.clear();

        VeterinaryPatient reactivated = patientRepositoryPort
                .findByVeterinaryIdAndPetId(veterinaryId, petId).orElseThrow();
        assertThat(reactivated.getId()).isEqualTo(patientId);
        assertThat(reactivated.getConsentStatus()).isEqualTo(ConsentStatus.GRANTED);
        assertThat(reactivated.getRevokedAt()).isNull();
        assertThat(reactivated.getUpdatedAt()).isAfter(revoked.getUpdatedAt());
    }

    @Test
    void patient_findGrantedByOwner_shouldReturnOnlyThatOwnersGrantedPets() {
        UUID veterinaryId = persistClinic();
        UUID ownerId = UUID.randomUUID();
        UUID otherOwnerId = UUID.randomUUID();

        UUID grantedPet = UUID.randomUUID();
        patientRepositoryPort.save(VeterinaryPatient.builder()
                .id(UUID.randomUUID()).veterinaryId(veterinaryId).petId(grantedPet)
                .grantedBy(ownerId).grantedAt(LocalDateTime.now()).createdAt(LocalDateTime.now())
                .petName("Max").build());
        patientRepositoryPort.save(VeterinaryPatient.builder()
                .id(UUID.randomUUID()).veterinaryId(veterinaryId).petId(UUID.randomUUID())
                .consentStatus(ConsentStatus.REVOKED)
                .grantedBy(ownerId).grantedAt(LocalDateTime.now().minusMonths(1))
                .revokedAt(LocalDateTime.now()).createdAt(LocalDateTime.now())
                .petName("Luna").build());
        patientRepositoryPort.save(VeterinaryPatient.builder()
                .id(UUID.randomUUID()).veterinaryId(veterinaryId).petId(UUID.randomUUID())
                .grantedBy(otherOwnerId).grantedAt(LocalDateTime.now()).createdAt(LocalDateTime.now())
                .petName("Rocky").build());
        entityManager.flush();

        List<VeterinaryPatient> shared =
                patientRepositoryPort.findGrantedByVeterinaryIdAndGrantedBy(veterinaryId, ownerId);

        assertThat(shared).extracting(VeterinaryPatient::getPetId).containsExactly(grantedPet);
        assertThat(patientRepositoryPort.countGrantedByVeterinaryIdAndGrantedBy(veterinaryId, ownerId))
                .isEqualTo(1);
        assertThat(patientRepositoryPort
                .countGrantedByVeterinaryIdAndGrantedBy(veterinaryId, otherOwnerId)).isEqualTo(1);
    }

    @Test
    void patient_shouldBeScopedToItsClinic() {
        UUID firstClinic = persistClinic();
        UUID secondClinic = persistClinic();
        UUID petId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        // La misma mascota compartida con dos clínicas: una fila por par, el único es (vet, pet).
        patientRepositoryPort.save(VeterinaryPatient.builder()
                .id(UUID.randomUUID()).veterinaryId(firstClinic).petId(petId)
                .grantedBy(ownerId).grantedAt(LocalDateTime.now()).createdAt(LocalDateTime.now())
                .petName("Max").build());
        patientRepositoryPort.save(VeterinaryPatient.builder()
                .id(UUID.randomUUID()).veterinaryId(secondClinic).petId(petId)
                .grantedBy(ownerId).grantedAt(LocalDateTime.now()).createdAt(LocalDateTime.now())
                .petName("Max").build());
        entityManager.flush();

        assertThat(patientRepositoryPort.findByVeterinaryIdAndPetId(firstClinic, petId)).isPresent();
        assertThat(patientRepositoryPort.findByVeterinaryIdAndPetId(secondClinic, petId)).isPresent();
        assertThat(patientRepositoryPort.findByVeterinaryIdAndPetId(UUID.randomUUID(), petId)).isEmpty();
    }
}

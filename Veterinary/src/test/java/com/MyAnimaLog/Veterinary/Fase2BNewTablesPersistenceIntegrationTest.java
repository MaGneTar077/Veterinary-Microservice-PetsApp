package com.MyAnimaLog.Veterinary;

import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.legal.ports.out.LegalConsentRepositoryPort;
import com.MyAnimaLog.Veterinary.application.professional.ports.out.ProfessionalProfileRepositoryPort;
import com.MyAnimaLog.Veterinary.application.verification.ports.out.VerificationDocumentRepositoryPort;
import com.MyAnimaLog.Veterinary.application.verification.ports.out.VerificationRequestRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.legal.enums.ConsentType;
import com.MyAnimaLog.Veterinary.domain.legal.model.LegalConsent;
import com.MyAnimaLog.Veterinary.domain.professional.enums.IdDocumentType;
import com.MyAnimaLog.Veterinary.domain.professional.enums.Profession;
import com.MyAnimaLog.Veterinary.domain.professional.model.ProfessionalProfile;
import com.MyAnimaLog.Veterinary.domain.verification.enums.SubjectType;
import com.MyAnimaLog.Veterinary.domain.verification.enums.VerificationRequestStatus;
import com.MyAnimaLog.Veterinary.domain.verification.model.VerificationDocument;
import com.MyAnimaLog.Veterinary.domain.verification.model.VerificationRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Saves each Fase 2B table's domain model with only the fields a real service would set
 * (relying on {@code @Builder.Default} for the rest) through the real Port -> Mapper -> Entity ->
 * Hibernate -> Supabase path. Catches the class of bug where a DB column is {@code NOT NULL} with
 * a SQL-side {@code DEFAULT}, but Hibernate still inserts an explicit {@code null} because nothing
 * on the Java side set it (confirmed in production: {@code RegisterVeterinary} failed with
 * "null value in column ai_status violates not-null constraint" because
 * {@code VerificationRequest.aiStatus} had no default before this fix).
 *
 * <p>{@code @Transactional} rolls every save back at the end of each test — nothing is left behind
 * in Supabase.
 *
 * <p><strong>Cada test tiene que leer lo que guardó</strong> (Fase 3): {@code save(...)} sobre una
 * entidad con id asignado no hace flush por sí solo, y {@code @Transactional} revierte antes del
 * commit, así que un test que solo guarda pasa en verde <em>sin haber ejecutado el INSERT</em> — no
 * prueba nada de la base. La lectura posterior fuerza el flush. Por lo mismo, las tablas con FK a
 * {@code public.veterinary} necesitan una clínica real ({@link TestClinics#persist}) y no un
 * {@code UUID} inventado.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class Fase2BNewTablesPersistenceIntegrationTest {

    @Autowired
    private LegalConsentRepositoryPort legalConsentRepositoryPort;

    @Autowired
    private ProfessionalProfileRepositoryPort professionalProfileRepositoryPort;

    @Autowired
    private VerificationRequestRepositoryPort verificationRequestRepositoryPort;

    @Autowired
    private VerificationDocumentRepositoryPort verificationDocumentRepositoryPort;

    @Autowired
    private VeterinaryRepositoryPort veterinaryRepositoryPort;

    @Test
    void legalConsent_shouldPersist_withOnlyTheFieldsRegisterVeterinarySets() {
        UUID userId = UUID.randomUUID();
        LegalConsent consent = LegalConsent.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                // Clínica real: legal_consents.veterinary_id tiene FK a public.veterinary.
                .veterinaryId(TestClinics.persist(veterinaryRepositoryPort))
                .consentType(ConsentType.DATA_TREATMENT)
                .version("2026-10-v1")
                // acceptedAt intentionally omitted: must fall back to @Builder.Default, not DB DEFAULT.
                .build();

        legalConsentRepositoryPort.save(consent);

        // La lectura fuerza el flush: sin ella el INSERT no llegaría a Postgres.
        List<LegalConsent> saved = legalConsentRepositoryPort.findByUserId(userId);
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getAcceptedAt()).isNotNull();
        assertThat(saved.get(0).getConsentType()).isEqualTo(ConsentType.DATA_TREATMENT);
    }

    @Test
    void professionalProfile_shouldPersist_withOnlyTheMinimalRequiredFields() {
        UUID userId = UUID.randomUUID();
        ProfessionalProfile profile = ProfessionalProfile.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .fullName("Integration Test Vet")
                .documentType(IdDocumentType.CC)
                .documentNumber("123456789")
                .licenseNumber(UUID.randomUUID().toString())
                .profession(Profession.MV)
                // licenseStatus/createdAt/updatedAt intentionally omitted.
                .build();

        professionalProfileRepositoryPort.save(profile);

        ProfessionalProfile saved = professionalProfileRepositoryPort.findByUserId(userId).orElseThrow();
        assertThat(saved.getLicenseStatus()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void verificationRequest_shouldPersist_withOnlyTheFieldsRegisterVeterinarySets() {
        VerificationRequest request = VerificationRequest.builder()
                .id(UUID.randomUUID())
                .subjectType(SubjectType.CLINIC)
                .subjectId(UUID.randomUUID())
                .status(VerificationRequestStatus.DRAFT)
                .submittedBy(UUID.randomUUID())
                // attemptNumber/aiStatus/registryChecked/createdAt intentionally omitted.
                .build();

        verificationRequestRepositoryPort.save(request);

        VerificationRequest saved = verificationRequestRepositoryPort.findById(request.getId()).orElseThrow();
        assertThat(saved.getAttemptNumber()).isNotNull();
        assertThat(saved.getAiStatus()).isNotNull();
        assertThat(saved.getRegistryChecked()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void verificationDocument_shouldPersist_withOnlyTheFieldsUploadServiceSets() {
        VerificationRequest request = verificationRequestRepositoryPort.save(VerificationRequest.builder()
                .id(UUID.randomUUID())
                .subjectType(SubjectType.CLINIC)
                .subjectId(UUID.randomUUID())
                .status(VerificationRequestStatus.DRAFT)
                .submittedBy(UUID.randomUUID())
                .build());

        VerificationDocument document = VerificationDocument.builder()
                .id(UUID.randomUUID())
                .requestId(request.getId())
                .documentType("RUT")
                .storagePath("verifications/" + request.getId() + "/doc.pdf")
                .mimeType("application/pdf")
                .fileSizeBytes(1024L)
                .sha256("0".repeat(64))
                // uploadedAt intentionally omitted.
                .build();

        verificationDocumentRepositoryPort.save(document);

        VerificationDocument saved = verificationDocumentRepositoryPort.findById(document.getId()).orElseThrow();
        assertThat(saved.getUploadedAt()).isNotNull();
        assertThat(saved.getRequestId()).isEqualTo(request.getId());
    }

    @Test
    void verificationDocument_shouldPersistAndReadBackOriginalFileName() {
        VerificationRequest request = verificationRequestRepositoryPort.save(VerificationRequest.builder()
                .id(UUID.randomUUID())
                .subjectType(SubjectType.PROFESSIONAL)
                .subjectId(UUID.randomUUID())
                .status(VerificationRequestStatus.DRAFT)
                .submittedBy(UUID.randomUUID())
                .build());

        VerificationDocument document = VerificationDocument.builder()
                .id(UUID.randomUUID())
                .requestId(request.getId())
                .documentType("PROFESSIONAL_CARD")
                .storagePath("verifications/" + request.getId() + "/doc.pdf")
                .mimeType("application/pdf")
                .fileSizeBytes(1024L)
                .sha256("0".repeat(64))
                .originalFileName("tarjeta profesional.pdf")
                .build();

        verificationDocumentRepositoryPort.save(document);

        VerificationDocument saved = verificationDocumentRepositoryPort.findById(document.getId()).orElseThrow();
        assertThat(saved.getOriginalFileName()).isEqualTo("tarjeta profesional.pdf");
        assertThat(saved.displayFileName()).isEqualTo("tarjeta profesional.pdf");
    }
}

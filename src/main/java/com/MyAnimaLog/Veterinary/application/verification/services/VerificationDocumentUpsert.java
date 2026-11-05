package com.MyAnimaLog.Veterinary.application.verification.services;

import com.MyAnimaLog.Veterinary.application.shared.ports.out.FileStoragePort;
import com.MyAnimaLog.Veterinary.application.verification.ports.out.VerificationDocumentRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.DocumentUploadConflictException;
import com.MyAnimaLog.Veterinary.domain.verification.model.VerificationDocument;
import com.MyAnimaLog.Veterinary.infrastructure.storage.FileNameSanitizer;
import com.MyAnimaLog.Veterinary.infrastructure.storage.FileSignatureDetector.DetectedType;
import com.MyAnimaLog.Veterinary.infrastructure.storage.Sha256;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Uploads (or replaces) one verification document, shared by the clinic and professional-profile
 * document services so a bug fixed in one of them can't silently stay broken in the other.
 *
 * <p>If a document of the same type already exists on the request, this reuses its row id and
 * {@code UPDATE}s it in place instead of inserting a new row — inserting a second row for the
 * same (request_id, document_type) trips {@code ux_verification_documents_request_type} and used
 * to surface as a raw 500 (fixed in the "reemplazar documento existente" bugfix). The new file is
 * uploaded to Storage before touching the DB; on any DB failure it's deleted again. The *old*
 * file (when the storage path actually changes, e.g. the extension changed) is only deleted
 * {@code afterCommit} — same pattern as {@code GooglePubSubEventAdapter} — so a rollback of the
 * surrounding {@code @Transactional} method never leaves the DB pointing at a file that was
 * deleted for nothing. Callers must run inside a transaction for that guarantee to apply; without
 * one (e.g. a pure Mockito unit test) the old file is deleted immediately instead.
 */
public final class VerificationDocumentUpsert {

    private static final Logger log = LoggerFactory.getLogger(VerificationDocumentUpsert.class);

    private VerificationDocumentUpsert() {
    }

    public static VerificationDocument upsert(VerificationDocumentRepositoryPort repositoryPort,
                                        FileStoragePort fileStoragePort,
                                        String bucket,
                                        UUID requestId,
                                        String documentType,
                                        byte[] content,
                                        DetectedType detectedType,
                                        String originalFileName) {
        Optional<VerificationDocument> existing = repositoryPort.findByRequestId(requestId)
                .stream()
                .filter(d -> d.getDocumentType().equals(documentType))
                .findFirst();

        UUID documentId = existing.map(VerificationDocument::getId).orElseGet(UUID::randomUUID);
        String storagePath = "verifications/" + requestId + "/" + documentId + "." + detectedType.extension();

        fileStoragePort.uploadPrivate(bucket, storagePath, content, detectedType.mimeType());

        VerificationDocument document = VerificationDocument.builder()
                .id(documentId)
                .requestId(requestId)
                .documentType(documentType)
                .storagePath(storagePath)
                .mimeType(detectedType.mimeType())
                .fileSizeBytes((long) content.length)
                .sha256(Sha256.hex(content))
                .uploadedAt(LocalDateTime.now())
                .originalFileName(FileNameSanitizer.sanitize(originalFileName))
                .build();

        try {
            repositoryPort.save(document);
        } catch (DataIntegrityViolationException e) {
            fileStoragePort.delete(bucket, storagePath);
            throw new DocumentUploadConflictException(
                    "Another upload for document type " + documentType + " is already in progress, try again");
        } catch (RuntimeException e) {
            fileStoragePort.delete(bucket, storagePath);
            throw e;
        }

        existing.filter(old -> !old.getStoragePath().equals(storagePath))
                .ifPresent(old -> scheduleOldFileDeletion(fileStoragePort, bucket, old.getStoragePath()));

        return document;
    }

    private static void scheduleOldFileDeletion(FileStoragePort fileStoragePort, String bucket, String path) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    deleteQuietly(fileStoragePort, bucket, path);
                }
            });
        } else {
            deleteQuietly(fileStoragePort, bucket, path);
        }
    }

    private static void deleteQuietly(FileStoragePort fileStoragePort, String bucket, String path) {
        try {
            fileStoragePort.delete(bucket, path);
        } catch (RuntimeException e) {
            log.error("Failed to delete replaced verification document from storage: bucket={}, path={}", bucket, path, e);
        }
    }
}

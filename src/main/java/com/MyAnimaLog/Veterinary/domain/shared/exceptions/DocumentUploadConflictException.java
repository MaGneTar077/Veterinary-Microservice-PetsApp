package com.MyAnimaLog.Veterinary.domain.shared.exceptions;

/**
 * Two uploads for the same (requestId, documentType) raced and both tried to insert a new row,
 * tripping the DB's unique constraint ({@code ux_verification_documents_request_type}). Thrown
 * instead of letting the {@code DataIntegrityViolationException} fall through to the generic 500
 * handler — shared by the clinic and professional-profile document upload services.
 */
public class DocumentUploadConflictException extends RuntimeException {

    public DocumentUploadConflictException() {
        super("Another upload for this document type is already in progress, try again");
    }

    public DocumentUploadConflictException(String message) {
        super(message);
    }
}

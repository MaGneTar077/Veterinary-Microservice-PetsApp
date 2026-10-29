package com.MyAnimaLog.Veterinary.infrastructure.config;

import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.EmailNotVerifiedException;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.InvalidNitException;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.InvalidVeterinaryEmailException;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.ClinicNotActiveException;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.ClinicNotSuspendedException;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.CannotManageOwnClinicException;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.InvalidSuspensionReasonException;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.TooManyOwnedVeterinariesException;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.VeterinaryAlreadyExistsException;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.VeterinaryEmailAlreadyExistsException;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.VeterinaryNitAlreadyExistsException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.CannotModifyOwnerException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.CannotModifyPeerAdminException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.CannotModifySelfException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.EmployeeAlreadyExistsException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.EmployeeNotFoundException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.InvalidEmployeeRoleException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.InvalidInvitationEmailException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.InvitationAlreadyPendingException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.InvitationEmailMismatchException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.InvitationExpiredException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.InvitationNotFoundException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.InvitationNotPendingException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.OnlyOwnerCanManageAdminsException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.OwnerCannotLeaveException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.NotClinicMemberException;
import com.MyAnimaLog.Veterinary.domain.subscription.exceptions.ActiveSubscriptionAlreadyExistsException;
import com.MyAnimaLog.Veterinary.domain.subscription.exceptions.NoActiveSubscriptionException;
import com.MyAnimaLog.Veterinary.domain.subscription.exceptions.SubscriptionNotFoundException;
import com.MyAnimaLog.Veterinary.domain.patients.exceptions.InvalidInviteCodeException;
import com.MyAnimaLog.Veterinary.domain.patients.exceptions.UserAlreadyLinkedException;
import com.MyAnimaLog.Veterinary.domain.patients.exceptions.UserNotLinkedException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.InvalidVeterinaryNameException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VeterinaryNotActiveException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VeterinaryNotFoundException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.ClinicContextRequiredException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.InsufficientPermissionException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.ClinicSuspendedException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.PlatformAdminRequiredException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.TenantMismatchException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.UnauthenticatedException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.ExternalServiceUnavailableException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.NotVeterinaryOwnerException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VerificationLockedException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.InvalidFileTypeException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.FileTooLargeException;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.ConsentRequiredException;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.ConsentVersionOutdatedException;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.InvalidVeterinaryFieldException;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.InvalidLegalRepresentativeException;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.InvalidDocumentTypeException;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.TruthfulnessDeclarationRequiredException;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.MissingRequiredDocumentsException;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.InvalidVerificationStateException;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.VerificationAttemptsExhaustedException;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.VerificationDocumentNotFoundException;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.VerificationRequestNotFoundException;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.RegistryCheckRequiredException;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.InvalidReviewDecisionException;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.CannotReviewOwnRequestException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.DocumentNotInCorrectionException;
import com.MyAnimaLog.Veterinary.domain.verification.exceptions.AiVerificationDisabledException;
import com.MyAnimaLog.Veterinary.domain.professional.exceptions.ProfessionalProfileNotFoundException;
import com.MyAnimaLog.Veterinary.domain.professional.exceptions.LicenseNumberAlreadyExistsException;
import com.MyAnimaLog.Veterinary.domain.professional.exceptions.InvalidProfessionalProfileException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String message, String code) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("code", code);
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(InvalidVeterinaryNameException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidName(InvalidVeterinaryNameException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), "VALIDATION_ERROR");
    }

    @ExceptionHandler(InvalidVeterinaryEmailException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidEmail(InvalidVeterinaryEmailException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), "VALIDATION_ERROR");
    }

    @ExceptionHandler(VeterinaryAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleAlreadyExists(VeterinaryAlreadyExistsException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), "VETERINARY_NAME_TAKEN");
    }

    @ExceptionHandler(VeterinaryEmailAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleEmailExists(VeterinaryEmailAlreadyExistsException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), "VETERINARY_EMAIL_TAKEN");
    }

    @ExceptionHandler(VeterinaryNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(VeterinaryNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), "VETERINARY_NOT_FOUND");
    }

    @ExceptionHandler(EmployeeAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleEmployeeAlreadyExists(EmployeeAlreadyExistsException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), "EMPLOYEE_ALREADY_EXISTS");
    }

    @ExceptionHandler(InvalidEmployeeRoleException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidRole(InvalidEmployeeRoleException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), "INVALID_EMPLOYEE_ROLE");
    }

    @ExceptionHandler(VeterinaryNotActiveException.class)
    public ResponseEntity<Map<String, Object>> handleNotActive(VeterinaryNotActiveException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), "VETERINARY_NOT_ACTIVE");
    }

    @ExceptionHandler(EmployeeNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleEmployeeNotFound(EmployeeNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), "EMPLOYEE_NOT_FOUND");
    }

    @ExceptionHandler(InvitationNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleInvitationNotFound(InvitationNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), "INVITATION_NOT_FOUND");
    }

    @ExceptionHandler(InvitationAlreadyPendingException.class)
    public ResponseEntity<Map<String, Object>> handleInvitationAlreadyPending(InvitationAlreadyPendingException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), "INVITATION_ALREADY_PENDING");
    }

    @ExceptionHandler(InvitationNotPendingException.class)
    public ResponseEntity<Map<String, Object>> handleInvitationNotPending(InvitationNotPendingException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), "INVITATION_NOT_PENDING");
    }

    @ExceptionHandler(NotClinicMemberException.class)
    public ResponseEntity<Map<String, Object>> handleNotClinicMember(NotClinicMemberException ex) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), "NOT_CLINIC_MEMBER");
    }

    @ExceptionHandler(OwnerCannotLeaveException.class)
    public ResponseEntity<Map<String, Object>> handleOwnerCannotLeave(OwnerCannotLeaveException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), "OWNER_MUST_TRANSFER_OWNERSHIP");
    }

    @ExceptionHandler(InvitationEmailMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleInvitationEmailMismatch(InvitationEmailMismatchException ex) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), "INVITATION_EMAIL_MISMATCH");
    }

    @ExceptionHandler(InvitationExpiredException.class)
    public ResponseEntity<Map<String, Object>> handleInvitationExpired(InvitationExpiredException ex) {
        return build(HttpStatus.GONE, ex.getMessage(), "INVITATION_EXPIRED");
    }

    @ExceptionHandler(InvalidInvitationEmailException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidInvitationEmail(InvalidInvitationEmailException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), "VALIDATION_ERROR");
    }

    @ExceptionHandler(OnlyOwnerCanManageAdminsException.class)
    public ResponseEntity<Map<String, Object>> handleOnlyOwnerCanManageAdmins(OnlyOwnerCanManageAdminsException ex) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), "ONLY_OWNER_CAN_MANAGE_ADMINS");
    }

    @ExceptionHandler(SubscriptionNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleSubscriptionNotFound(SubscriptionNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), "SUBSCRIPTION_NOT_FOUND");
    }

    @ExceptionHandler(NoActiveSubscriptionException.class)
    public ResponseEntity<Map<String, Object>> handleNoActiveSubscription(NoActiveSubscriptionException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), "NO_ACTIVE_SUBSCRIPTION");
    }

    @ExceptionHandler(ActiveSubscriptionAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleActiveSubscriptionAlreadyExists(ActiveSubscriptionAlreadyExistsException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), "ACTIVE_SUBSCRIPTION_ALREADY_EXISTS");
    }

    @ExceptionHandler(InvalidInviteCodeException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidInviteCode(InvalidInviteCodeException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), "INVALID_INVITE_CODE");
    }

    @ExceptionHandler(UserAlreadyLinkedException.class)
    public ResponseEntity<Map<String, Object>> handleUserAlreadyLinked(UserAlreadyLinkedException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), "USER_ALREADY_LINKED");
    }

    @ExceptionHandler(UserNotLinkedException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotLinked(UserNotLinkedException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), "USER_NOT_LINKED");
    }

    @ExceptionHandler(UnauthenticatedException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthenticated(UnauthenticatedException ex) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), "UNAUTHENTICATED");
    }

    @ExceptionHandler(ClinicContextRequiredException.class)
    public ResponseEntity<Map<String, Object>> handleClinicContextRequired(ClinicContextRequiredException ex) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), "CLINIC_CONTEXT_REQUIRED");
    }

    @ExceptionHandler(TenantMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTenantMismatch(TenantMismatchException ex) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), "TENANT_MISMATCH");
    }

    @ExceptionHandler(InsufficientPermissionException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientPermission(InsufficientPermissionException ex) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), "INSUFFICIENT_PERMISSION");
    }

    @ExceptionHandler(PlatformAdminRequiredException.class)
    public ResponseEntity<Map<String, Object>> handlePlatformAdminRequired(PlatformAdminRequiredException ex) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), "PLATFORM_ADMIN_REQUIRED");
    }

    @ExceptionHandler(ClinicSuspendedException.class)
    public ResponseEntity<Map<String, Object>> handleClinicSuspended(ClinicSuspendedException ex) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), "CLINIC_SUSPENDED");
    }

    @ExceptionHandler(CannotModifySelfException.class)
    public ResponseEntity<Map<String, Object>> handleCannotModifySelf(CannotModifySelfException ex) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), "CANNOT_MODIFY_SELF");
    }

    @ExceptionHandler(CannotModifyOwnerException.class)
    public ResponseEntity<Map<String, Object>> handleCannotModifyOwner(CannotModifyOwnerException ex) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), "CANNOT_MODIFY_OWNER");
    }

    @ExceptionHandler(CannotModifyPeerAdminException.class)
    public ResponseEntity<Map<String, Object>> handleCannotModifyPeerAdmin(CannotModifyPeerAdminException ex) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), "CANNOT_MODIFY_PEER_ADMIN");
    }

    @ExceptionHandler(InvalidNitException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidNit(InvalidNitException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), "INVALID_NIT");
    }

    @ExceptionHandler(VeterinaryNitAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleNitAlreadyExists(VeterinaryNitAlreadyExistsException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), "NIT_TAKEN");
    }

    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<Map<String, Object>> handleEmailNotVerified(EmailNotVerifiedException ex) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), "EMAIL_NOT_VERIFIED");
    }

    @ExceptionHandler(TooManyOwnedVeterinariesException.class)
    public ResponseEntity<Map<String, Object>> handleTooManyOwnedVeterinaries(TooManyOwnedVeterinariesException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), "OWNER_CLINIC_LIMIT_REACHED");
    }

    @ExceptionHandler(ClinicNotActiveException.class)
    public ResponseEntity<Map<String, Object>> handleClinicNotActive(ClinicNotActiveException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), "CLINIC_NOT_ACTIVE");
    }

    @ExceptionHandler(ClinicNotSuspendedException.class)
    public ResponseEntity<Map<String, Object>> handleClinicNotSuspended(ClinicNotSuspendedException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), "CLINIC_NOT_SUSPENDED");
    }

    @ExceptionHandler(CannotManageOwnClinicException.class)
    public ResponseEntity<Map<String, Object>> handleCannotManageOwnClinic(CannotManageOwnClinicException ex) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), "CANNOT_MANAGE_OWN_CLINIC");
    }

    @ExceptionHandler(InvalidSuspensionReasonException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidSuspensionReason(InvalidSuspensionReasonException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), "VALIDATION_ERROR");
    }

    @ExceptionHandler(ExternalServiceUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleExternalServiceUnavailable(ExternalServiceUnavailableException ex) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), "EXTERNAL_SERVICE_UNAVAILABLE");
    }

    @ExceptionHandler(NotVeterinaryOwnerException.class)
    public ResponseEntity<Map<String, Object>> handleNotVeterinaryOwner(NotVeterinaryOwnerException ex) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), "NOT_VETERINARY_OWNER");
    }

    @ExceptionHandler(VerificationLockedException.class)
    public ResponseEntity<Map<String, Object>> handleVerificationLocked(VerificationLockedException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), "VERIFICATION_LOCKED");
    }

    @ExceptionHandler(InvalidFileTypeException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidFileType(InvalidFileTypeException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), "INVALID_FILE_TYPE");
    }

    @ExceptionHandler(FileTooLargeException.class)
    public ResponseEntity<Map<String, Object>> handleFileTooLarge(FileTooLargeException ex) {
        return build(HttpStatus.PAYLOAD_TOO_LARGE, ex.getMessage(), "FILE_TOO_LARGE");
    }

    @ExceptionHandler(ConsentRequiredException.class)
    public ResponseEntity<Map<String, Object>> handleConsentRequired(ConsentRequiredException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), "CONSENT_REQUIRED");
    }

    @ExceptionHandler(ConsentVersionOutdatedException.class)
    public ResponseEntity<Map<String, Object>> handleConsentVersionOutdated(ConsentVersionOutdatedException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), "CONSENT_VERSION_OUTDATED");
    }

    @ExceptionHandler(InvalidVeterinaryFieldException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidVeterinaryField(InvalidVeterinaryFieldException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), "VALIDATION_ERROR");
    }

    @ExceptionHandler(InvalidLegalRepresentativeException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidLegalRepresentative(InvalidLegalRepresentativeException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), "VALIDATION_ERROR");
    }

    @ExceptionHandler(InvalidDocumentTypeException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidDocumentType(InvalidDocumentTypeException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), "VALIDATION_ERROR");
    }

    @ExceptionHandler(TruthfulnessDeclarationRequiredException.class)
    public ResponseEntity<Map<String, Object>> handleTruthfulnessDeclarationRequired(TruthfulnessDeclarationRequiredException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), "TRUTHFULNESS_DECLARATION_REQUIRED");
    }

    @ExceptionHandler(MissingRequiredDocumentsException.class)
    public ResponseEntity<Map<String, Object>> handleMissingRequiredDocuments(MissingRequiredDocumentsException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), "MISSING_REQUIRED_DOCUMENTS");
    }

    @ExceptionHandler(InvalidVerificationStateException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidVerificationState(InvalidVerificationStateException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), "INVALID_VERIFICATION_STATE");
    }

    @ExceptionHandler(VerificationAttemptsExhaustedException.class)
    public ResponseEntity<Map<String, Object>> handleVerificationAttemptsExhausted(VerificationAttemptsExhaustedException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), "VERIFICATION_ATTEMPTS_EXHAUSTED");
    }

    @ExceptionHandler(VerificationDocumentNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleVerificationDocumentNotFound(VerificationDocumentNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), "DOCUMENT_NOT_FOUND");
    }

    @ExceptionHandler(VerificationRequestNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleVerificationRequestNotFound(VerificationRequestNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), "VERIFICATION_REQUEST_NOT_FOUND");
    }

    @ExceptionHandler(ProfessionalProfileNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleProfessionalProfileNotFound(ProfessionalProfileNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), "PROFESSIONAL_PROFILE_NOT_FOUND");
    }

    @ExceptionHandler(LicenseNumberAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleLicenseNumberAlreadyExists(LicenseNumberAlreadyExistsException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), "LICENSE_NUMBER_TAKEN");
    }

    @ExceptionHandler(InvalidProfessionalProfileException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidProfessionalProfile(InvalidProfessionalProfileException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), "VALIDATION_ERROR");
    }

    @ExceptionHandler(RegistryCheckRequiredException.class)
    public ResponseEntity<Map<String, Object>> handleRegistryCheckRequired(RegistryCheckRequiredException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), "REGISTRY_CHECK_REQUIRED");
    }

    @ExceptionHandler(InvalidReviewDecisionException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidReviewDecision(InvalidReviewDecisionException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), "VALIDATION_ERROR");
    }

    @ExceptionHandler(CannotReviewOwnRequestException.class)
    public ResponseEntity<Map<String, Object>> handleCannotReviewOwnRequest(CannotReviewOwnRequestException ex) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), "CANNOT_REVIEW_OWN_REQUEST");
    }

    @ExceptionHandler(DocumentNotInCorrectionException.class)
    public ResponseEntity<Map<String, Object>> handleDocumentNotInCorrection(DocumentNotInCorrectionException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), "DOCUMENT_NOT_IN_CORRECTION");
    }

    @ExceptionHandler(AiVerificationDisabledException.class)
    public ResponseEntity<Map<String, Object>> handleAiVerificationDisabled(AiVerificationDisabledException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), "AI_VERIFICATION_DISABLED");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        if (message.isBlank()) {
            message = "Validation failed";
        }
        return build(HttpStatus.BAD_REQUEST, message, "VALIDATION_ERROR");
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<Map<String, Object>> handleNoResourceFound(Exception ex) {
        return build(HttpStatus.NOT_FOUND, "No handler found for this route", "NOT_FOUND");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.error("Data integrity violation", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno, intenta de nuevo", "INTERNAL_ERROR");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        log.error("Unexpected error", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno, intenta de nuevo", "INTERNAL_ERROR");
    }
}

package com.MyAnimaLog.Veterinary.infrastructure.config;

import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.EmailNotVerifiedException;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.InvalidNitException;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.InvalidVeterinaryEmailException;
import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.InvalidVeterinaryStatusTransitionException;
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
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.PlatformAdminRequiredException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.TenantMismatchException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.UnauthenticatedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message
        ));
    }

    @ExceptionHandler(InvalidVeterinaryNameException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidName(InvalidVeterinaryNameException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(InvalidVeterinaryEmailException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidEmail(InvalidVeterinaryEmailException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(VeterinaryAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleAlreadyExists(VeterinaryAlreadyExistsException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(VeterinaryEmailAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleEmailExists(VeterinaryEmailAlreadyExistsException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error: " + ex.getMessage());
    }

    @ExceptionHandler(VeterinaryNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(VeterinaryNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(EmployeeAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleEmployeeAlreadyExists(EmployeeAlreadyExistsException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(InvalidEmployeeRoleException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidRole(InvalidEmployeeRoleException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(VeterinaryNotActiveException.class)
    public ResponseEntity<Map<String, Object>> handleNotActive(VeterinaryNotActiveException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(EmployeeNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleEmployeeNotFound(EmployeeNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(SubscriptionNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleSubscriptionNotFound(SubscriptionNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(NoActiveSubscriptionException.class)
    public ResponseEntity<Map<String, Object>> handleNoActiveSubscription(NoActiveSubscriptionException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ActiveSubscriptionAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleActiveSubscriptionAlreadyExists(ActiveSubscriptionAlreadyExistsException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(InvalidInviteCodeException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidInviteCode(InvalidInviteCodeException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(UserAlreadyLinkedException.class)
    public ResponseEntity<Map<String, Object>> handleUserAlreadyLinked(UserAlreadyLinkedException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(UserNotLinkedException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotLinked(UserNotLinkedException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(UnauthenticatedException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthenticated(UnauthenticatedException ex) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(ClinicContextRequiredException.class)
    public ResponseEntity<Map<String, Object>> handleClinicContextRequired(ClinicContextRequiredException ex) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(TenantMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTenantMismatch(TenantMismatchException ex) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(InsufficientPermissionException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientPermission(InsufficientPermissionException ex) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(PlatformAdminRequiredException.class)
    public ResponseEntity<Map<String, Object>> handlePlatformAdminRequired(PlatformAdminRequiredException ex) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(CannotModifySelfException.class)
    public ResponseEntity<Map<String, Object>> handleCannotModifySelf(CannotModifySelfException ex) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(CannotModifyOwnerException.class)
    public ResponseEntity<Map<String, Object>> handleCannotModifyOwner(CannotModifyOwnerException ex) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(CannotModifyPeerAdminException.class)
    public ResponseEntity<Map<String, Object>> handleCannotModifyPeerAdmin(CannotModifyPeerAdminException ex) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(InvalidNitException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidNit(InvalidNitException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(VeterinaryNitAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleNitAlreadyExists(VeterinaryNitAlreadyExistsException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<Map<String, Object>> handleEmailNotVerified(EmailNotVerifiedException ex) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(TooManyOwnedVeterinariesException.class)
    public ResponseEntity<Map<String, Object>> handleTooManyOwnedVeterinaries(TooManyOwnedVeterinariesException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(InvalidVeterinaryStatusTransitionException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidStatusTransition(InvalidVeterinaryStatusTransitionException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }
}
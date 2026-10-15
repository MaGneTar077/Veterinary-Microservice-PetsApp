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
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.ExternalServiceUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
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

    @ExceptionHandler(InvalidVeterinaryStatusTransitionException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidStatusTransition(InvalidVeterinaryStatusTransitionException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), "INVALID_VERIFICATION_STATE");
    }

    @ExceptionHandler(ExternalServiceUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleExternalServiceUnavailable(ExternalServiceUnavailableException ex) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), "EXTERNAL_SERVICE_UNAVAILABLE");
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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error: " + ex.getMessage(), "INTERNAL_ERROR");
    }
}

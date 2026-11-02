package com.MyAnimaLog.Veterinary.infrastructure.security;

import com.MyAnimaLog.Veterinary.application.shared.dto.AuthenticatedUser;
import com.MyAnimaLog.Veterinary.application.shared.dto.ClinicContext;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.AuthenticatedUserPort;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.UnauthenticatedException;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class SpringAuthenticatedUserAdapter implements AuthenticatedUserPort {

    @Override
    public AuthenticatedUser current() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
            throw new UnauthenticatedException();
        }
        Jwt jwt = jwtAuthentication.getToken();

        return new AuthenticatedUser(
                UUID.fromString(jwt.getSubject()),
                jwt.getClaimAsString("email"),
                Boolean.TRUE.equals(jwt.getClaim("email_verified")),
                "PLATFORM_ADMIN".equals(jwt.getClaimAsString("platform_role")),
                clinicContextOf(jwt)
        );
    }

    private Optional<ClinicContext> clinicContextOf(Jwt jwt) {
        if (!"VETERINARY".equals(jwt.getClaimAsString("ctx"))) {
            return Optional.empty();
        }
        String employeeIdClaim = jwt.getClaimAsString("employee_id");
        return Optional.of(new ClinicContext(
                UUID.fromString(jwt.getClaimAsString("vet_id")),
                employeeIdClaim != null ? UUID.fromString(employeeIdClaim) : null,
                // TODO(VET-09): vet_role solo trae hoy VETERINARIAN/ASSISTANT/ADMIN porque el
                // User service todavia no emite tokens de clinica (los crea VET-11 en adelante);
                // cuando exista OWNER/RECEPTIONIST, EmployeeRole debe soportarlos tambien.
                EmployeeRole.valueOf(jwt.getClaimAsString("vet_role")),
                Boolean.TRUE.equals(jwt.getClaim("vet_licensed")),
                VeterinaryStatus.valueOf(jwt.getClaimAsString("vet_status"))
        ));
    }
}

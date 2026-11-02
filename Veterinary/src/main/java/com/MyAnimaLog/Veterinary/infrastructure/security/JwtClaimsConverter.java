package com.MyAnimaLog.Veterinary.infrastructure.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts the raw JWT into a {@link JwtAuthenticationToken}, granting
 * {@code PLATFORM_ADMIN} when {@code platform_role=PLATFORM_ADMIN} and
 * {@code VET_ROLE_<rol>} when {@code ctx=VETERINARY}, per CONTRATOS_COMPARTIDOS.md §1.
 */
@Component
public class JwtClaimsConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        if ("PLATFORM_ADMIN".equals(jwt.getClaimAsString("platform_role"))) {
            authorities.add(new SimpleGrantedAuthority("PLATFORM_ADMIN"));
        }

        if ("VETERINARY".equals(jwt.getClaimAsString("ctx"))) {
            String vetRole = jwt.getClaimAsString("vet_role");
            if (vetRole != null) {
                authorities.add(new SimpleGrantedAuthority("VET_ROLE_" + vetRole));
            }
        }

        return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
    }
}

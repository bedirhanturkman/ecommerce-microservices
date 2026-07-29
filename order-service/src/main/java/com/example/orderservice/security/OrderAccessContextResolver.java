package com.example.orderservice.security;

import com.example.orderservice.exception.InvalidCustomerClaimException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class OrderAccessContextResolver {

    public OrderAccessContext resolve(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken jwtToken)) {
            throw new InvalidCustomerClaimException();
        }

        Object claim = jwtToken.getToken().getClaim("customerId");

        if (!(claim instanceof Number customerIdClaim)) {
            throw new InvalidCustomerClaimException();
        }

        long customerId = customerIdClaim.longValue();

        if (customerId <= 0) {
            throw new InvalidCustomerClaimException();
        }

        boolean admin = authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals(
                                "ROLE_" + PermissionConstants.ROLE_ADMIN
                        )
                );

        return new OrderAccessContext(customerId, admin);
    }
}

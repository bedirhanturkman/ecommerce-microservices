package com.example.customerservice.security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service("permissionService")
public class PermissionService {

    public boolean isAdmin(Authentication authentication) {
        return hasRole(authentication, PermissionConstants.ROLE_ADMIN);
    }

    public boolean isUser(Authentication authentication) {
        return hasRole(authentication, PermissionConstants.ROLE_USER);
    }

    public boolean isUserOrAdmin(Authentication authentication) {
        return isUser(authentication) || isAdmin(authentication);
    }

    private boolean hasRole(Authentication authentication, String role) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        String authority = "ROLE_" + role;

        return authentication.getAuthorities()
                .stream()
                .anyMatch(grantedAuthority ->
                        grantedAuthority.getAuthority().equals(authority)
                );
    }
}
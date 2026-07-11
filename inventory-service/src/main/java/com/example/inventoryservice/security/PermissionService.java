package com.example.inventoryservice.security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service("permissionService")
public class PermissionService {

    private static final String ROLE_PREFIX = "ROLE_";

    public boolean isAdmin(Authentication authentication) {
        return hasRole(authentication, PermissionConstants.ROLE_ADMIN);
    }

    public boolean isUser(Authentication authentication) {
        return hasRole(authentication, PermissionConstants.ROLE_USER);
    }

    public boolean isSeller(Authentication authentication) {
        return hasRole(authentication, PermissionConstants.ROLE_SELLER);
    }

    public boolean isAdminOrSeller(Authentication authentication) {
        return isAdmin(authentication) || isSeller(authentication);
    }

    public boolean isUserOrAdminOrSeller(Authentication authentication) {
        return isUser(authentication)
                || isAdmin(authentication)
                || isSeller(authentication);
    }

    private boolean hasRole(
            Authentication authentication,
            String role
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        String expectedAuthority = ROLE_PREFIX + role;

        return authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        expectedAuthority.equals(authority.getAuthority())
                );
    }
}
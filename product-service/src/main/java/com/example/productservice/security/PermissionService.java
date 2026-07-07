package com.example.productservice.security;

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
                        grantedAuthority.getAuthority().equals(authority));
    }

    public boolean isSeller(Authentication authentication) {
        return hasRole(authentication, PermissionConstants.ROLE_SELLER);
    }

    public boolean isAdminOrSeller(Authentication authentication) {
        return isAdmin(authentication) || isSeller(authentication);
    }

    public boolean isUserOrAdminOrSeller(Authentication authentication) {
        return isUser(authentication) || isAdmin(authentication) || isSeller(authentication);
    }
}
package com.example.productservice.security;

public final class PermissionConstants {

    private PermissionConstants() {
    }

    public static final String ROLE_USER = "USER";
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_SELLER = "SELLER";

    public static final String HAS_ROLE_ADMIN =
            "hasRole('" + ROLE_ADMIN + "')";

    public static final String HAS_ROLE_ADMIN_OR_SELLER =
            "hasAnyRole('" + ROLE_ADMIN + "', '" + ROLE_SELLER + "')";

    public static final String HAS_ROLE_USER_OR_ADMIN_OR_SELLER =
            "hasAnyRole('" + ROLE_USER + "', '" + ROLE_ADMIN + "', '" + ROLE_SELLER + "')";
}
package com.example.inventoryservice.security;

public final class PermissionConstants {

    private PermissionConstants() {
    }

    public static final String ROLE_USER = "USER";
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_SELLER = "SELLER";

    public static final String IS_ADMIN =
            "@permissionService.isAdmin(authentication)";

    public static final String IS_ADMIN_OR_SELLER =
            "@permissionService.isAdminOrSeller(authentication)";

    public static final String IS_USER_OR_ADMIN_OR_SELLER =
            "@permissionService.isUserOrAdminOrSeller(authentication)";
}
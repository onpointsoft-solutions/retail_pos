package com.retailpos.util;

import at.favre.lib.crypto.bcrypt.BCrypt;

public class PasswordUtil {

    private PasswordUtil() {}

    /**
     * Hash a plaintext password using BCrypt with cost factor 12.
     */
    public static String hash(String password) {
        return BCrypt.withDefaults().hashToString(12, password.toCharArray());
    }

    /**
     * Verify a plaintext password against a BCrypt hash.
     */
    public static boolean verify(String password, String hash) {
        if (password == null || hash == null) return false;
        return BCrypt.verifyer().verify(password.toCharArray(), hash).verified;
    }
}

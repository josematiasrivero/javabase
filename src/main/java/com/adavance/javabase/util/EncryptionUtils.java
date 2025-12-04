package com.adavance.javabase.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class EncryptionUtils {

    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    private EncryptionUtils() {
        // Private constructor to hide the implicit public one
    }

    public static String encrypt(String password) {
        return encoder.encode(password);
    }

    public static boolean verify(String password, String hash) {
        return encoder.matches(password, hash);
    }
}

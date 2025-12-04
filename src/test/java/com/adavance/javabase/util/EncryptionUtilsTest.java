package com.adavance.javabase.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EncryptionUtilsTest {

    @Test
    void testEncryptionAndVerification() {
        String password = "mySecretPassword";
        String encrypted = EncryptionUtils.encrypt(password);

        assertNotNull(encrypted);
        assertNotEquals(password, encrypted);
        assertTrue(EncryptionUtils.verify(password, encrypted));
        assertFalse(EncryptionUtils.verify("wrongPassword", encrypted));
    }
}

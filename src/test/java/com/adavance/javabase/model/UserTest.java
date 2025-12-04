package com.adavance.javabase.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void testPasswordEncryptionOnCreate() {
        User user = new User();
        user.setRawPassword("password123");
        
        // Simulate PrePersist
        user.onCreate();
        
        assertNotNull(user.getEncryptedPassword(), "Encrypted password should not be null");
        assertNotEquals("password123", user.getEncryptedPassword(), "Password should be encrypted");
    }

    @Test
    void testPasswordEncryptionOnUpdate() {
        User user = new User();
        user.setRawPassword("newPassword456");
        
        // Simulate PreUpdate
        user.onUpdate();
        
        assertNotNull(user.getEncryptedPassword(), "Encrypted password should not be null");
        assertNotEquals("newPassword456", user.getEncryptedPassword(), "Password should be encrypted");
    }
}

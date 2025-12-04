package com.adavance.javabase.model;

import com.adavance.javabase.util.EncryptionUtils;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User extends BaseEntity {

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String encryptedPassword;

    @Transient
    private String rawPassword;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();


    @Override
    protected void beforeOnCreate() {
        if (rawPassword != null) {
            this.encryptedPassword = EncryptionUtils.encrypt(rawPassword);
        }
    }

    @Override
    protected void beforeOnUpdate() {
        if (rawPassword != null) {
            this.encryptedPassword = EncryptionUtils.encrypt(rawPassword);
        }
    }
}

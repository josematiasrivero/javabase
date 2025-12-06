package com.adavance.javabase.model;

import com.adavance.javabase.util.EncryptionUtils;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@Accessors(chain = true)
public class User extends BaseEntity {

    protected User() {
    }

    public User(String username, String rawPassword, List<Role> roles) {
        this.username = username;
        this.rawPassword = rawPassword;
        this.roles = new HashSet<>(roles);
    }

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String encryptedPassword;

    @Transient
    private String rawPassword;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles;

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

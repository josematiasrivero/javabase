package com.adavance.javabase.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.Instant;
import java.util.UUID;

@MappedSuperclass
@Getter
@Setter
@Accessors(chain = true)
public class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    private Long id;

    @Column(unique = true, nullable = false, updatable = false)
    protected String uuid;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        beforeOnCreate();

        if (uuid == null) {
            uuid = UUID.randomUUID().toString();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
    }

    protected void beforeOnCreate() {
    };

    @PreUpdate
    protected void onUpdate() {
        beforeOnUpdate();
        updatedAt = Instant.now();
    }

    protected void beforeOnUpdate() {
    };

}

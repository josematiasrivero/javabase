package com.adavance.javabase.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "roles")
@Getter
@Setter
@Accessors(chain = true)
public class Role extends BaseEntity {

    protected Role() {
    }

    public Role(String uuid, String name, List<String> permissions) {
        this.uuid = uuid;
        this.name = name;
        this.permissions = permissions;
    }

    @Column(unique = true, nullable = false)
    private String name;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "role_permissions", joinColumns = @JoinColumn(name = "role_id"))
    @Column(name = "permission")
    private List<String> permissions = new ArrayList<>();

}

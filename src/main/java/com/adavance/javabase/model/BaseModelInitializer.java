package com.adavance.javabase.model;

import com.adavance.javabase.annotations.ObjectStore;
import com.adavance.javabase.repository.GenericRepository;

import jakarta.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
@ObjectStore
public class BaseModelInitializer extends ModelInitializer {

    public static Role ADMIN_ROLE;

    public static User ADMIN_USER;

    public BaseModelInitializer(GenericRepository genericRepository) {
        super(genericRepository);
    }
    
    @PostConstruct
    public void init() {
        ADMIN_ROLE = ensureByUuid(new Role("admin", "admin", List.of("User.*")));
        ADMIN_USER = ensureByUuid(new User("admin", "admin", "password", List.of(ADMIN_ROLE)));
    }
}

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

    public static Role ADMIN_ROLE = new Role("admin", List.of("User.*"));

    public static User ADMIN_USER = new User("admin", "admin", List.of(ADMIN_ROLE));

    public BaseModelInitializer(GenericRepository genericRepository) {
        super(genericRepository);
    }
    
    @PostConstruct
    public void init() {
        ensurePersisted(Set.of("ADMIN_ROLE", "ADMIN_USER"));
    }
}

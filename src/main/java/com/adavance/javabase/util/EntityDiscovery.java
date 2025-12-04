package com.adavance.javabase.util;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.Metamodel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility class to discover all @Entity classes in the application.
 */
@Component
@RequiredArgsConstructor
public class EntityDiscovery {

    private final EntityManagerFactory entityManagerFactory;
    private final Map<String, Class<?>> entityNameToClass = new HashMap<>();
    private final Map<Class<?>, String> classToEntityName = new HashMap<>();

    @PostConstruct
    public void discoverEntities() {
        // Use JPA's metamodel to discover all entities
        Metamodel metamodel = entityManagerFactory.getMetamodel();
        
        for (EntityType<?> entityType : metamodel.getEntities()) {
            Class<?> entityClass = entityType.getJavaType();
            if (entityClass.isAnnotationPresent(Entity.class)) {
                String entityName = toEntityName(entityClass.getSimpleName());
                entityNameToClass.put(entityName, entityClass);
                classToEntityName.put(entityClass, entityName);
            }
        }
    }

    /**
     * Converts a class name to a kebab-case entity name.
     * Example: AddOnLevel -> add-on-level
     */
    private String toEntityName(String className) {
        return className
                .replaceAll("([a-z])([A-Z])", "$1-$2")
                .toLowerCase();
    }

    /**
     * Get all discovered entity classes.
     */
    public Set<Class<?>> getAllEntityClasses() {
        return entityNameToClass.values().stream().collect(Collectors.toSet());
    }

    /**
     * Get entity class by name (e.g., "add-on-level").
     */
    public Optional<Class<?>> getEntityClass(String entityName) {
        return Optional.ofNullable(entityNameToClass.get(entityName.toLowerCase()));
    }

    /**
     * Get entity name for a class.
     */
    public Optional<String> getEntityName(Class<?> clazz) {
        return Optional.ofNullable(classToEntityName.get(clazz));
    }

    /**
     * Check if an entity name exists.
     */
    public boolean hasEntity(String entityName) {
        return entityNameToClass.containsKey(entityName.toLowerCase());
    }
}


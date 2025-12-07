package com.adavance.javabase.model;

import com.adavance.javabase.repository.GenericRepository;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Abstract base class for model initializers.
 * Provides common functionality for ensuring static fields are persisted in the database.
 */
@Slf4j
public abstract class ModelInitializer {

    protected final GenericRepository genericRepository;
    private final Set<Set<String>> processedFieldSets = Collections.synchronizedSet(new HashSet<>());

    public ModelInitializer(GenericRepository genericRepository) {
        this.genericRepository = genericRepository;
    }

    /**
     * Ensures an entity exists by UUID. If an entity with the given UUID exists, returns it.
     * If it doesn't exist, creates it and returns it.
     *
     * @param entity the entity to ensure exists
     * @return the existing entity if found by UUID, or the newly created entity
     */
    protected <T extends BaseEntity> T ensureByUuid(T entity) {
        return genericRepository.ensureByUuid(entity);
    }

}


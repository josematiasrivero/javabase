package com.adavance.javabase.model;

import com.adavance.javabase.repository.GenericRepository;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Abstract base class for model initializers.
 * Provides common functionality for ensuring static fields are persisted in the database.
 */
public abstract class ModelInitializer {

    protected final GenericRepository genericRepository;
    private final Set<Set<String>> processedFieldSets = Collections.synchronizedSet(new HashSet<>());

    public ModelInitializer(GenericRepository genericRepository) {
        this.genericRepository = genericRepository;
    }

    /**
     * Ensures that static fields are persisted in the database.
     * For each field name, gets the static field, checks if it's persisted by UUID,
     * and if not, persists it. Then updates the static field with the persisted entity.
     * This method will only process each unique set of field names once.
     *
     * @param fieldNames set of strings representing static field names
     */
    protected void ensurePersisted(Set<String> fieldNames) {
        // Create an immutable copy to use as a key
        Set<String> fieldNamesKey = Set.copyOf(fieldNames);
        
        // Check if this set of field names has already been processed
        if (!processedFieldSets.add(fieldNamesKey)) {
            return; // Already processed, skip
        }

        for (String fieldName : fieldNames) {
            try {
                // Get the static field by name
                Field field = this.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);

                // Get the current entity value
                Object entity = field.get(null);

                if (entity == null) {
                    continue;
                }

                // Persist if not already persisted by UUID
                Object persistedEntity = genericRepository.saveIfNotExistsByUuid(entity);

                // Update the static field with the persisted entity
                field.set(null, persistedEntity);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException("Failed to ensure persistence for field: " + fieldName, e);
            }
        }
    }
}


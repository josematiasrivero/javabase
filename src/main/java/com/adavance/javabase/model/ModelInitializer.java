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
            log.debug("Field set {} already processed, skipping", fieldNames);
            return; // Already processed, skip
        }

        log.info("Ensuring persistence for {} field(s): {}", fieldNames.size(), fieldNames);

        for (String fieldName : fieldNames) {
            try {
                log.debug("Processing field: {}", fieldName);
                
                // Get the static field by name
                Field field = this.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);

                // Get the current entity value
                Object entity = field.get(null);

                if (entity == null) {
                    log.warn("Field {} is null, skipping", fieldName);
                    continue;
                }

                if (!(entity instanceof BaseEntity)) {
                    log.warn("Field {} is not a BaseEntity, skipping", fieldName);
                    continue;
                }

                log.debug("Persisting entity for field {}: {}", fieldName, entity.getClass().getSimpleName());
                
                // Ensure entity exists by UUID (return existing if found, create if not)
                BaseEntity persistedEntity = genericRepository.ensureByUuid((BaseEntity) entity);

                // Update the static field with the persisted entity
                field.set(null, persistedEntity);
                
                log.info("Successfully persisted and updated field: {}", fieldName);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                log.error("Failed to ensure persistence for field: {}", fieldName, e);
                throw new RuntimeException("Failed to ensure persistence for field: " + fieldName, e);
            }
        }
        
        log.info("Completed ensuring persistence for {} field(s)", fieldNames.size());
    }
}


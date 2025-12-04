package com.adavance.javabase.controller;

import com.adavance.javabase.util.EntityDiscovery;
import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.bind.annotation.*;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Generic REST controller that handles all entity requests under /rest/*
 * 
 * Automatically discovers all @Entity classes and provides CRUD operations:
 * - GET /rest/{entity-name} - List all entities
 * - GET /rest/{entity-name}/{id} - Get entity by ID
 * - POST /rest/{entity-name} - Create new entity
 */
@RestController
@RequestMapping("/rest")
@RequiredArgsConstructor
@Slf4j
public class GenericRestController {

    private final EntityDiscovery entityDiscovery;
    private final EntityManager entityManager;

    /**
     * GET /rest/{entityName}
     * Returns all entities of the specified type.
     */
    @GetMapping("/{entityName}")
    public ResponseEntity<?> getAllEntities(@PathVariable String entityName) {
        log.debug("GET /rest/{} - Listing all entities", entityName);
        
        Optional<Class<?>> entityClassOpt = entityDiscovery.getEntityClass(entityName);
        if (entityClassOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Entity not found: " + entityName));
        }

        try {
            Class<?> entityClass = entityClassOpt.get();
            String jpql = "SELECT e FROM " + entityClass.getSimpleName() + " e";
            TypedQuery<?> query = entityManager.createQuery(jpql, entityClass);
            List<?> results = query.getResultList();
            
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("Error fetching entities for {}", entityName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch entities: " + e.getMessage()));
        }
    }

    /**
     * GET /rest/{entityName}/{id}
     * Returns a specific entity by ID.
     */
    @GetMapping("/{entityName}/{id}")
    public ResponseEntity<?> getEntityById(@PathVariable String entityName, @PathVariable Long id) {
        log.debug("GET /rest/{}/{} - Getting entity by ID", entityName, id);
        
        Optional<Class<?>> entityClassOpt = entityDiscovery.getEntityClass(entityName);
        if (entityClassOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Entity not found: " + entityName));
        }

        try {
            Class<?> entityClass = entityClassOpt.get();
            Object entity = entityManager.find(entityClass, id);
            
            if (entity == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Entity with ID " + id + " not found"));
            }
            
            return ResponseEntity.ok(entity);
        } catch (Exception e) {
            log.error("Error fetching entity {} with ID {}", entityName, id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch entity: " + e.getMessage()));
        }
    }

    /**
     * POST /rest/{entityName}
     * Creates a new entity from the request body.
     */
    @PostMapping("/{entityName}")
    @Transactional
    public ResponseEntity<?> createEntity(
            @PathVariable String entityName,
            @RequestBody Map<String, Object> requestBody) {
        log.debug("POST /rest/{} - Creating new entity", entityName);
        
        Optional<Class<?>> entityClassOpt = entityDiscovery.getEntityClass(entityName);
        if (entityClassOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Entity not found: " + entityName));
        }

        try {
            Class<?> entityClass = entityClassOpt.get();
            
            // Create new instance of the entity
            Object entity = entityClass.getDeclaredConstructor().newInstance();
            
            // Set fields from request body using reflection
            setEntityFields(entity, requestBody, entityClass);
            
            // Persist the entity
            entityManager.persist(entity);
            entityManager.flush();
            
            log.info("Successfully created entity {}: {}", entityName, entity);
            return ResponseEntity.status(HttpStatus.CREATED).body(entity);
        } catch (PersistenceException e) {
            // Database constraint violations - mark transaction for rollback
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error("Database constraint violation creating entity {}", entityName, e);
            // Get the root cause for a more helpful error message
            Throwable rootCause = e.getCause();
            String errorMessage = rootCause != null ? rootCause.getMessage() : e.getMessage();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Database constraint violation: " + errorMessage));
        } catch (Exception e) {
            // Other exceptions - also mark for rollback to be safe
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error("Error creating entity {}", entityName, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to create entity: " + e.getMessage()));
        }
    }

    /**
     * Sets entity fields from a Map using reflection.
     * Handles common types like String, Integer, Long, BigDecimal, Boolean, etc.
     * Also handles JPA relationships (@ManyToOne, @OneToOne) by loading related entities.
     */
    private void setEntityFields(Object entity, Map<String, Object> data, Class<?> entityClass) throws Exception {
        // Get all fields including inherited ones
        Class<?> currentClass = entityClass;
        while (currentClass != null && currentClass != Object.class) {
            Field[] fields = currentClass.getDeclaredFields();
            
            for (Field field : fields) {
                String fieldName = field.getName();
                
                // Skip JPA-managed fields (id, uuid, createdAt, updatedAt)
                if (fieldName.equals("id") || fieldName.equals("uuid") || 
                    fieldName.equals("createdAt") || fieldName.equals("updatedAt")) {
                    continue;
                }
                
                field.setAccessible(true);
                Class<?> fieldType = field.getType();
                
                // Check if this is a JPA relationship field
                boolean isRelationship = field.isAnnotationPresent(ManyToOne.class) || 
                                        field.isAnnotationPresent(OneToOne.class);
                
                Object value = null;
                
                // For relationships, check for fieldName or fieldNameId
                if (isRelationship) {
                    if (data.containsKey(fieldName)) {
                        value = data.get(fieldName);
                    } else if (data.containsKey(fieldName + "Id")) {
                        // Try to load entity by ID
                        Object idValue = data.get(fieldName + "Id");
                        if (idValue != null) {
                            value = entityManager.find(fieldType, convertToLong(idValue));
                        }
                    }
                } else {
                    // For regular fields, only use the exact field name
                    if (data.containsKey(fieldName)) {
                        value = data.get(fieldName);
                    }
                }
                
                if (value == null) {
                    continue;
                }
                
                // Handle relationship fields that might be passed as Map with "id"
                if (isRelationship && value instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> relationMap = (Map<String, Object>) value;
                    if (relationMap.containsKey("id")) {
                        Object idValue = relationMap.get("id");
                        value = entityManager.find(fieldType, convertToLong(idValue));
                    }
                }
                
                // Convert value to appropriate type
                Object convertedValue = convertValue(value, fieldType);
                field.set(entity, convertedValue);
            }
            
            currentClass = currentClass.getSuperclass();
        }
    }
    
    /**
     * Converts a value to Long for entity ID lookups.
     */
    private Long convertToLong(Object value) {
        if (value instanceof Long) {
            return (Long) value;
        } else if (value instanceof Number) {
            return ((Number) value).longValue();
        } else {
            return Long.parseLong(value.toString());
        }
    }

    /**
     * Converts a value to the target type.
     */
    private Object convertValue(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }
        
        // If already the correct type, return as is
        if (targetType.isInstance(value)) {
            return value;
        }
        
        // Handle common type conversions
        if (targetType == String.class) {
            return value.toString();
        } else if (targetType == Integer.class || targetType == int.class) {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            return Integer.parseInt(value.toString());
        } else if (targetType == Long.class || targetType == long.class) {
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
            return Long.parseLong(value.toString());
        } else if (targetType == Boolean.class || targetType == boolean.class) {
            if (value instanceof Boolean) {
                return value;
            }
            return Boolean.parseBoolean(value.toString());
        } else if (targetType == BigDecimal.class) {
            if (value instanceof Number) {
                return BigDecimal.valueOf(((Number) value).doubleValue());
            }
            return new BigDecimal(value.toString());
        } else if (targetType == Instant.class) {
            if (value instanceof String) {
                return Instant.parse((String) value);
            }
        }
        
        // For other types, try to return as is (might work for nested objects)
        return value;
    }
}


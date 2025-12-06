package com.adavance.javabase.controller;

import com.adavance.javabase.util.EntityDiscovery;
import io.swagger.v3.oas.annotations.Hidden;
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
 * - GET /rest/{entity-name}/{uuid} - Get entity by UUID
 * - POST /rest/{entity-name} - Create new entity
 * - PUT /rest/{entity-name}/{uuid} - Update entity
 * - DELETE /rest/{entity-name}/{uuid} - Delete entity
 */
@Hidden
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
     * GET /rest/{entityName}/{uuid}
     * Returns a specific entity by UUID.
     */
    @GetMapping("/{entityName}/{uuid}")
    public ResponseEntity<?> getEntityByUuid(@PathVariable String entityName, @PathVariable String uuid) {
        log.debug("GET /rest/{}/{} - Getting entity by UUID", entityName, uuid);

        Optional<Class<?>> entityClassOpt = entityDiscovery.getEntityClass(entityName);
        if (entityClassOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Entity not found: " + entityName));
        }

        try {
            Class<?> entityClass = entityClassOpt.get();
            Optional<?> entityOpt = findEntityByUuid(entityClass, uuid);

            if (entityOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Entity with UUID " + uuid + " not found"));
            }

            return ResponseEntity.ok(entityOpt.get());
        } catch (Exception e) {
            log.error("Error fetching entity {} with UUID {}", entityName, uuid, e);
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
     * PUT /rest/{entityName}/{uuid}
     * Updates an existing entity.
     */
    @PutMapping("/{entityName}/{uuid}")
    @Transactional
    public ResponseEntity<?> updateEntity(
            @PathVariable String entityName,
            @PathVariable String uuid,
            @RequestBody Map<String, Object> requestBody) {
        log.debug("PUT /rest/{}/{} - Updating entity", entityName, uuid);

        Optional<Class<?>> entityClassOpt = entityDiscovery.getEntityClass(entityName);
        if (entityClassOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Entity not found: " + entityName));
        }

        try {
            Class<?> entityClass = entityClassOpt.get();
            Optional<?> entityOpt = findEntityByUuid(entityClass, uuid);

            if (entityOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Entity with UUID " + uuid + " not found"));
            }

            Object entity = entityOpt.get();

            // Update fields from request body
            setEntityFields(entity, requestBody, entityClass);

            // Merge the entity
            entityManager.merge(entity);
            entityManager.flush();

            log.info("Successfully updated entity {}: {}", entityName, entity);
            return ResponseEntity.ok(entity);
        } catch (PersistenceException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error("Database constraint violation updating entity {}", entityName, e);
            Throwable rootCause = e.getCause();
            String errorMessage = rootCause != null ? rootCause.getMessage() : e.getMessage();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Database constraint violation: " + errorMessage));
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error("Error updating entity {}", entityName, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to update entity: " + e.getMessage()));
        }
    }

    /**
     * DELETE /rest/{entityName}/{uuid}
     * Deletes an entity by UUID.
     */
    @DeleteMapping("/{entityName}/{uuid}")
    @Transactional
    public ResponseEntity<?> deleteEntity(@PathVariable String entityName, @PathVariable String uuid) {
        log.debug("DELETE /rest/{}/{} - Deleting entity", entityName, uuid);

        Optional<Class<?>> entityClassOpt = entityDiscovery.getEntityClass(entityName);
        if (entityClassOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Entity not found: " + entityName));
        }

        try {
            Class<?> entityClass = entityClassOpt.get();
            Optional<?> entityOpt = findEntityByUuid(entityClass, uuid);

            if (entityOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Entity with UUID " + uuid + " not found"));
            }

            Object entity = entityOpt.get();
            entityManager.remove(entity);

            log.info("Successfully deleted entity {}: {}", entityName, uuid);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error("Error deleting entity {}", entityName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete entity: " + e.getMessage()));
        }
    }

    /**
     * Helper to find an entity by UUID using JPQL.
     */
    private Optional<?> findEntityByUuid(Class<?> entityClass, String uuid) {
        try {
            String jpql = "SELECT e FROM " + entityClass.getSimpleName() + " e WHERE e.uuid = :uuid";
            TypedQuery<?> query = entityManager.createQuery(jpql, entityClass);
            query.setParameter("uuid", uuid);
            return Optional.of(query.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    /**
     * Sets entity fields from a Map using reflection.
     * Handles common types like String, Integer, Long, BigDecimal, Boolean, etc.
     * Also handles JPA relationships (@ManyToOne, @OneToOne) by loading related
     * entities.
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
                boolean isManyToOne = field.isAnnotationPresent(ManyToOne.class);
                boolean isOneToOne = field.isAnnotationPresent(OneToOne.class);
                boolean isOneToMany = field.isAnnotationPresent(OneToMany.class);
                boolean isManyToMany = field.isAnnotationPresent(ManyToMany.class);
                boolean isRelationship = isManyToOne || isOneToOne || isOneToMany || isManyToMany;

                Object value = null;

                // For relationships, check for fieldName or fieldNameId
                if (isRelationship) {
                    if (data.containsKey(fieldName)) {
                        value = data.get(fieldName);
                    } else if ((isManyToOne || isOneToOne) && data.containsKey(fieldName + "Id")) {
                        // Try to load entity by UUID or ID
                        Object idValue = data.get(fieldName + "Id");
                        if (idValue != null) {
                            if (idValue instanceof String) {
                                // Assume UUID
                                Optional<?> relatedEntity = findEntityByUuid(fieldType, (String) idValue);
                                if (relatedEntity.isPresent()) {
                                    value = relatedEntity.get();
                                } else {
                                    throw new IllegalArgumentException("Related entity " + fieldType.getSimpleName()
                                            + " with UUID " + idValue + " not found");
                                }
                            } else {
                                // Fallback to ID (Long) if passed as number
                                value = entityManager.find(fieldType, convertToLong(idValue));
                            }
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

                // Handle OneToMany/ManyToMany (List of entities)
                if ((isOneToMany || isManyToMany) && value instanceof List) {
                    java.lang.reflect.ParameterizedType stringListType = (java.lang.reflect.ParameterizedType) field
                            .getGenericType();
                    Class<?> relatedClass = (Class<?>) stringListType.getActualTypeArguments()[0];

                    List<Object> relatedEntities = new java.util.ArrayList<>();
                    List<?> listValue = (List<?>) value;

                    for (Object item : listValue) {
                        if (item instanceof Map) {
                            // Create new instance or find existing
                            @SuppressWarnings("unchecked")
                            Map<String, Object> itemMap = (Map<String, Object>) item;

                            Object relatedEntity;
                            if (itemMap.containsKey("uuid")) {
                                String uuid = (String) itemMap.get("uuid");
                                Optional<?> existing = findEntityByUuid(relatedClass, uuid);
                                if (existing.isPresent()) {
                                    relatedEntity = existing.get();
                                    // Update existing entity fields if needed?
                                    // For now, let's assume we just link it, or update it if it's owned.
                                    // If it's OneToMany with Cascade.ALL, we might want to update it.
                                    setEntityFields(relatedEntity, itemMap, relatedClass);
                                } else {
                                    throw new IllegalArgumentException("Related entity " + relatedClass.getSimpleName()
                                            + " with UUID " + uuid + " not found");
                                }
                            } else {
                                // Create new
                                relatedEntity = relatedClass.getDeclaredConstructor().newInstance();
                                setEntityFields(relatedEntity, itemMap, relatedClass);
                            }
                            relatedEntities.add(relatedEntity);
                        } else if (item instanceof String) {
                            // Assume UUID
                            Optional<?> existing = findEntityByUuid(relatedClass, (String) item);
                            if (existing.isPresent()) {
                                relatedEntities.add(existing.get());
                            } else {
                                throw new IllegalArgumentException("Related entity " + relatedClass.getSimpleName()
                                        + " with UUID " + item + " not found");
                            }
                        }
                    }

                    field.set(entity, relatedEntities);
                    continue;
                }

                // Handle relationship fields that might be passed as Map with "uuid" or "id"
                if ((isManyToOne || isOneToOne) && value instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> relationMap = (Map<String, Object>) value;
                    if (relationMap.containsKey("uuid")) {
                        String relatedUuid = (String) relationMap.get("uuid");
                        Optional<?> relatedEntity = findEntityByUuid(fieldType, relatedUuid);
                        if (relatedEntity.isPresent()) {
                            value = relatedEntity.get();
                        } else {
                            throw new IllegalArgumentException("Related entity " + fieldType.getSimpleName()
                                    + " with UUID " + relatedUuid + " not found");
                        }
                    } else if (relationMap.containsKey("id")) {
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

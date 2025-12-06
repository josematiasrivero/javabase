package com.adavance.javabase.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.adavance.javabase.model.BaseEntity;

import java.util.List;
import java.util.Optional;

/**
 * Generic repository for performing CRUD operations on any entity type.
 * Provides type-safe operations using reflection and JPQL.
 */
@Repository
@Slf4j
public class GenericRepository {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Finds all entities of the specified type.
     *
     * @param entityClass the entity class
     * @return list of all entities
     */
    public <T extends BaseEntity> List<T> findAll(Class<T> entityClass) {
        String jpql = "SELECT e FROM " + entityClass.getSimpleName() + " e";
        TypedQuery<T> query = entityManager.createQuery(jpql, entityClass);
        return query.getResultList();
    }

    /**
     * Finds an entity by UUID.
     *
     * @param entityClass the entity class
     * @param uuid the UUID to search for
     * @return Optional containing the entity if found, empty otherwise
     */
    public <T extends BaseEntity> Optional<T> findByUuid(Class<T> entityClass, String uuid) {
        try {
            String jpql = "SELECT e FROM " + entityClass.getSimpleName() + " e WHERE e.uuid = :uuid";
            TypedQuery<T> query = entityManager.createQuery(jpql, entityClass);
            query.setParameter("uuid", uuid);
            return Optional.of(query.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    /**
     * Persists a new entity.
     *
     * @param entity the entity to persist
     * @return the persisted entity
     */
    @Transactional
    public <T extends BaseEntity> T save(T entity) {
        entityManager.persist(entity);
        entityManager.flush();
        return entity;
    }

    /**
     * Updates an existing entity.
     *
     * @param entity the entity to update
     * @return the updated entity
     */
    @Transactional
    public <T extends BaseEntity> T update(T entity) {
        T merged = entityManager.merge(entity);
        entityManager.flush();
        return merged;
    }

    /**
     * Deletes an entity.
     *
     * @param entity the entity to delete
     */
    @Transactional
    public void delete(BaseEntity entity) {
        entityManager.remove(entity);
    }

    /**
     * Finds an entity by ID (Long).
     *
     * @param entityClass the entity class
     * @param id the ID to search for
     * @return the entity if found, null otherwise
     */
    public <T extends BaseEntity> T findById(Class<T> entityClass, Long id) {
        return entityManager.find(entityClass, id);
    }

    /**
     * Ensures an entity exists by UUID. If an entity with the given UUID exists, returns it.
     * If it doesn't exist, creates it and returns it.
     *
     * @param entity the entity to ensure exists
     * @return the existing entity if found by UUID, or the newly created entity
     */
    @Transactional
    @SuppressWarnings("unchecked")
    public <T extends BaseEntity> T ensureByUuid(T entity) {
        if (entity == null) {
            return null;
        }

        if (entity.getUuid() != null) {
            // Check if entity with this UUID already exists
            Optional<T> existing = findByUuid((Class<T>) entity.getClass(), entity.getUuid());
            if (existing.isPresent()) {
                return existing.get();
            }
        }

        // Entity doesn't exist or has no UUID, create it
        return save(entity);
    }
}

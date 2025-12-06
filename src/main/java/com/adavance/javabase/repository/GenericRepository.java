package com.adavance.javabase.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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
    public List<?> findAll(Class<?> entityClass) {
        String jpql = "SELECT e FROM " + entityClass.getSimpleName() + " e";
        TypedQuery<?> query = entityManager.createQuery(jpql, entityClass);
        return query.getResultList();
    }

    /**
     * Finds an entity by UUID.
     *
     * @param entityClass the entity class
     * @param uuid the UUID to search for
     * @return Optional containing the entity if found, empty otherwise
     */
    public Optional<?> findByUuid(Class<?> entityClass, String uuid) {
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
     * Persists a new entity.
     *
     * @param entity the entity to persist
     * @return the persisted entity
     */
    @Transactional
    public Object save(Object entity) {
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
    public Object update(Object entity) {
        Object merged = entityManager.merge(entity);
        entityManager.flush();
        return merged;
    }

    /**
     * Deletes an entity.
     *
     * @param entity the entity to delete
     */
    @Transactional
    public void delete(Object entity) {
        entityManager.remove(entity);
    }

    /**
     * Finds an entity by ID (Long).
     *
     * @param entityClass the entity class
     * @param id the ID to search for
     * @return the entity if found, null otherwise
     */
    public Object findById(Class<?> entityClass, Long id) {
        return entityManager.find(entityClass, id);
    }
}


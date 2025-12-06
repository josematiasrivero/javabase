package com.adavance.javabase.model;

import com.adavance.javabase.annotations.ObjectStore;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@ObjectStore
public class BaseModelInitializer {

    public static Role ADMIN_ROLE = new Role("admin", List.of("User.*"));

    public static User ADMIN_USER = new User("admin", "admin", List.of(ADMIN_ROLE));

    private static EntityManager entityManager;
    private static TransactionTemplate transactionTemplate;

    @PersistenceContext
    public void setEntityManager(EntityManager entityManager) {
        BaseModelInitializer.entityManager = entityManager;
    }

    @Autowired
    public void setTransactionTemplate(PlatformTransactionManager transactionManager) {
        BaseModelInitializer.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public static void init() {
        //ensurePersisted("ADMIN_ROLE", "ADMIN_USER");
    }
}

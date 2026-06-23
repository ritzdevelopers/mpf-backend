package com.mypropertyfact.estate.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceException;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Legacy {@code blogs_chk_1} only allowed status 0–1. Draft (2) and scheduled (3) need the
 * constraint widened before {@link com.mypropertyfact.estate.services.BlogServiceImpl} can persist them.
 */
@Component
@Order(1)
@RequiredArgsConstructor
public class BlogStatusCheckConstraintRunner implements ApplicationRunner {

    private final EntityManager entityManager;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (constraintAllowsExtendedStatuses()) {
            return;
        }

        try {
            entityManager.createNativeQuery("ALTER TABLE blogs DROP CHECK blogs_chk_1")
                    .executeUpdate();
        } catch (PersistenceException ignored) {
            // Constraint may already be absent on fresh databases.
        }

        try {
            entityManager.createNativeQuery(
                            "ALTER TABLE blogs ADD CONSTRAINT blogs_chk_1 CHECK (status >= 0 AND status <= 3)")
                    .executeUpdate();
        } catch (PersistenceException ignored) {
            // Another instance may have applied the migration already.
        }
    }

    private boolean constraintAllowsExtendedStatuses() {
        try {
            Object row = entityManager.createNativeQuery(
                            """
                            SELECT cc.CHECK_CLAUSE
                            FROM information_schema.TABLE_CONSTRAINTS tc
                            JOIN information_schema.CHECK_CONSTRAINTS cc
                              ON tc.CONSTRAINT_SCHEMA = cc.CONSTRAINT_SCHEMA
                             AND tc.CONSTRAINT_NAME = cc.CONSTRAINT_NAME
                            WHERE tc.CONSTRAINT_SCHEMA = DATABASE()
                              AND tc.TABLE_NAME = 'blogs'
                              AND tc.CONSTRAINT_TYPE = 'CHECK'
                              AND tc.CONSTRAINT_NAME = 'blogs_chk_1'
                            """)
                    .getSingleResult();
            if (row == null) {
                return false;
            }
            String clause = row.toString().replace(" ", "");
            return clause.contains("<=3");
        } catch (NoResultException ignored) {
            return false;
        }
    }
}

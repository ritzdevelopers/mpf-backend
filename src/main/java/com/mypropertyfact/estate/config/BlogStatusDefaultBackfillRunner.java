package com.mypropertyfact.estate.config;

import com.mypropertyfact.estate.repositories.BlogRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Legacy blogs were saved with {@code status = 0} because the admin form did not send status.
 * When every row is still inactive, activate them once so the default is visible on the website.
 * After admins use the status toggle, mixed active/inactive rows are left unchanged.
 */
@Component
@Order(2)
@RequiredArgsConstructor
public class BlogStatusDefaultBackfillRunner implements ApplicationRunner {

    private final BlogRepository blogRepository;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        long total = blogRepository.count();
        if (total == 0) {
            return;
        }

        long inactive = blogRepository.countByStatus(0);
        if (inactive != total) {
            return;
        }

        entityManager.createNativeQuery("UPDATE blogs SET status = 1 WHERE status = 0")
                .executeUpdate();
    }
}

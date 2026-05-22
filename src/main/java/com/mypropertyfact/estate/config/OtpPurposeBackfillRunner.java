package com.mypropertyfact.estate.config;

import jakarta.persistence.EntityManager;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Existing OTP rows created before {@code purpose} existed must behave as {@link com.mypropertyfact.estate.entities.OtpPurpose#MAGIC_LINK}.
 */
@Component
@Order(1)
@RequiredArgsConstructor
public class OtpPurposeBackfillRunner implements ApplicationRunner {

    private final EntityManager entityManager;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        entityManager.createNativeQuery(
                        "UPDATE otp SET purpose = 'MAGIC_LINK' WHERE purpose IS NULL OR purpose = ''")
                .executeUpdate();
    }
}

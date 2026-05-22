package com.mypropertyfact.estate.services;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MpfDataChangeDetectorService {

    /**
     * Physical MySQL table names (must match {@code @Table} / Hibernate defaults in this project).
     */
    private static final List<String> ACTIVITY_TABLES = List.of(
            "projects",
            "property_listings",
            "blogs",
            "web_story",
            "builders",
            "enquiries",
            "home_banners",
            "amenity",
            "features",
            "cities",
            "career_applications",
            "users");

    private final EntityManager entityManager;

    /**
     * Latest activity timestamp across CMS + public-facing business tables.
     */
    public Optional<LocalDateTime> latestDataActivityAt() {
        LocalDateTime max = null;
        for (String table : ACTIVITY_TABLES) {
            if (!tableExists(table)) {
                continue;
            }
            Optional<LocalDateTime> tableMax = maxTimestampForTable(table);
            if (tableMax.isPresent() && (max == null || tableMax.get().isAfter(max))) {
                max = tableMax.get();
            }
        }
        return Optional.ofNullable(max);
    }

    public boolean hasDataChangesSince(LocalDateTime since) {
        if (since == null) {
            return latestDataActivityAt().isPresent();
        }
        return latestDataActivityAt()
                .map(latest -> latest.isAfter(since))
                .orElse(false);
    }

    private boolean tableExists(String tableName) {
        try {
            Number count = (Number) entityManager
                    .createNativeQuery(
                            """
                            SELECT COUNT(*) FROM information_schema.tables
                            WHERE table_schema = DATABASE() AND table_name = :tableName
                            """)
                    .setParameter("tableName", tableName)
                    .getSingleResult();
            return count != null && count.intValue() > 0;
        } catch (Exception e) {
            log.warn("Could not check table {}: {}", tableName, e.getMessage());
            return false;
        }
    }

    private Optional<LocalDateTime> maxTimestampForTable(String tableName) {
        try {
            String sql =
                    "users".equals(tableName)
                            ? "SELECT MAX(created_at) FROM " + tableName
                            : "SELECT MAX(COALESCE(updated_at, created_at)) FROM " + tableName;
            Object result = entityManager.createNativeQuery(sql).getSingleResult();
            return toLocalDateTime(result);
        } catch (Exception e) {
            log.warn("Activity check skipped for table {}: {}", tableName, e.getMessage());
            return Optional.empty();
        }
    }

    private static Optional<LocalDateTime> toLocalDateTime(Object result) {
        if (result == null) {
            return Optional.empty();
        }
        if (result instanceof LocalDateTime ldt) {
            return Optional.of(ldt);
        }
        if (result instanceof java.sql.Timestamp ts) {
            return Optional.of(ts.toLocalDateTime());
        }
        return Optional.empty();
    }
}

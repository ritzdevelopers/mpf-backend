-- Super Admin project listing dashboard.
-- Hibernate ddl-auto=update also creates these on startup.

-- Website project creator (nullable for legacy rows).
-- ALTER TABLE projects ADD COLUMN created_by_user_id INT NULL;

CREATE TABLE IF NOT EXISTS listing_activity_event (
    id BIGINT NOT NULL AUTO_INCREMENT,
    source VARCHAR(16) NOT NULL,
    entity_id BIGINT NOT NULL,
    action VARCHAR(32) NOT NULL,
    actor_user_id INT NULL,
    actor_name VARCHAR(255) NULL,
    detail VARCHAR(500) NULL,
    occurred_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_listing_activity_source_entity (source, entity_id, occurred_at),
    KEY idx_listing_activity_occurred (occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

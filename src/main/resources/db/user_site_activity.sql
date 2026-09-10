CREATE TABLE IF NOT EXISTS user_site_activity (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id INT NOT NULL,
  activity_type VARCHAR(32) NOT NULL,
  entity_type VARCHAR(32) NULL,
  entity_id VARCHAR(64) NULL,
  entity_slug VARCHAR(255) NULL,
  entity_label VARCHAR(255) NULL,
  href VARCHAR(500) NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_user_activity_user_type (user_id, activity_type, updated_at),
  KEY idx_user_activity_lookup (user_id, activity_type, entity_type, entity_slug)
);

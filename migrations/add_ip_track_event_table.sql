-- IP visitor / scanner tracking (auto-created by Hibernate ddl-auto=update as well).
CREATE TABLE IF NOT EXISTS ip_track_event (
  id BIGINT NOT NULL AUTO_INCREMENT,
  occurred_at DATETIME(6) NOT NULL,
  remote_addr VARCHAR(64) NOT NULL,
  path VARCHAR(512) NOT NULL,
  http_method VARCHAR(16) NULL,
  user_agent VARCHAR(512) NULL,
  country VARCHAR(64) NULL,
  region VARCHAR(64) NULL,
  city VARCHAR(64) NULL,
  latitude DOUBLE NULL,
  longitude DOUBLE NULL,
  org VARCHAR(255) NULL,
  is_scan BIT(1) NOT NULL,
  source VARCHAR(32) NULL,
  PRIMARY KEY (id),
  KEY idx_ip_track_occurred (occurred_at),
  KEY idx_ip_track_addr_occurred (remote_addr, occurred_at),
  KEY idx_ip_track_scan_occurred (is_scan, occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

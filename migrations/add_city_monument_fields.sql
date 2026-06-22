-- Run once against the MPF database to add city monument/showcase fields.
ALTER TABLE cities
    ADD COLUMN IF NOT EXISTS monument_name VARCHAR(255) NULL,
    ADD COLUMN IF NOT EXISTS monument_image VARCHAR(512) NULL,
    ADD COLUMN IF NOT EXISTS city_highlights TEXT NULL,
    ADD COLUMN IF NOT EXISTS is_active TINYINT(1) NOT NULL DEFAULT 1;

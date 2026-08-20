-- Run once against the MPF database after deploying the Web Stories removal.
-- Drops leftover CMS tables; Hibernate ddl-auto=update will not remove them.

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS web_story;
DROP TABLE IF EXISTS web_story_category;
SET FOREIGN_KEY_CHECKS = 1;

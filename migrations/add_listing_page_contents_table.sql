-- Run once against the MPF database to add listing page SEO content support.
CREATE TABLE IF NOT EXISTS listing_page_contents (
    id INT AUTO_INCREMENT PRIMARY KEY,
    page_slug VARCHAR(255) NOT NULL,
    page_title VARCHAR(512) NULL,
    heading VARCHAR(512) NULL,
    intro TEXT NULL,
    content LONGTEXT NULL,
    meta_title VARCHAR(512) NULL,
    meta_description TEXT NULL,
    meta_keywords TEXT NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NULL,
    updated_at DATETIME NULL,
    UNIQUE KEY uk_listing_page_contents_slug (page_slug),
    INDEX idx_listing_page_contents_slug (page_slug)
);

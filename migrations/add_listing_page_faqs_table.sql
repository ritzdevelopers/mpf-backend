-- Run once against the MPF database to add listing page FAQ support.
CREATE TABLE IF NOT EXISTS listing_page_faqs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    page_slug VARCHAR(255) NOT NULL,
    page_title VARCHAR(512) NULL,
    faq_question LONGTEXT NOT NULL,
    faq_answer LONGTEXT NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NULL,
    updated_at DATETIME NULL,
    INDEX idx_listing_page_faqs_slug (page_slug)
);

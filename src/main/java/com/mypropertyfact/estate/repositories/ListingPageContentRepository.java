package com.mypropertyfact.estate.repositories;

import com.mypropertyfact.estate.entities.ListingPageContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ListingPageContentRepository extends JpaRepository<ListingPageContent, Integer> {
    Optional<ListingPageContent> findByPageSlug(String pageSlug);

    Optional<ListingPageContent> findByPageSlugAndIsActiveTrue(String pageSlug);

    List<ListingPageContent> findAllByOrderByPageSlugAsc();

    @Query(value = """
            SELECT
              id,
              page_slug,
              page_title,
              heading,
              meta_title,
              is_active,
              CASE WHEN
                (content IS NOT NULL AND content <> '')
                OR (intro IS NOT NULL AND intro <> '')
                OR (heading IS NOT NULL AND heading <> '')
                OR (meta_title IS NOT NULL AND meta_title <> '')
                OR (meta_description IS NOT NULL AND meta_description <> '')
                OR (meta_keywords IS NOT NULL AND meta_keywords <> '')
              THEN 1 ELSE 0 END AS has_content
            FROM listing_page_contents
            WHERE (:q IS NULL OR :q = ''
              OR LOWER(page_slug) LIKE LOWER(CONCAT('%', :q, '%'))
              OR LOWER(IFNULL(page_title, '')) LIKE LOWER(CONCAT('%', :q, '%')))
            ORDER BY page_slug ASC
            """, nativeQuery = true)
    List<Object[]> findAllSummaries(@Param("q") String q);
}

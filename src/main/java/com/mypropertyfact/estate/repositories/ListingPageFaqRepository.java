package com.mypropertyfact.estate.repositories;

import com.mypropertyfact.estate.entities.ListingPageFaq;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ListingPageFaqRepository extends JpaRepository<ListingPageFaq, Integer> {
    List<ListingPageFaq> findByPageSlugAndIsActiveTrueOrderBySortOrderAscIdAsc(String pageSlug);

    List<ListingPageFaq> findAllByOrderByPageSlugAscSortOrderAscIdAsc();

    @Query(
            value = """
            SELECT f.pageSlug, MIN(f.pageTitle), COUNT(f)
            FROM ListingPageFaq f
            GROUP BY f.pageSlug
            ORDER BY f.pageSlug ASC
            """,
            countQuery = """
            SELECT COUNT(DISTINCT f.pageSlug)
            FROM ListingPageFaq f
            """
    )
    Page<Object[]> findPageFaqSummaries(Pageable pageable);

    @Query("""
            SELECT f FROM ListingPageFaq f
            WHERE f.pageSlug IN :pageSlugs
            ORDER BY f.pageSlug ASC, f.sortOrder ASC, f.id ASC
            """)
    List<ListingPageFaq> findByPageSlugs(@Param("pageSlugs") Collection<String> pageSlugs);
}

package com.mypropertyfact.estate.repositories;

import com.mypropertyfact.estate.entities.ListingPageContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ListingPageContentRepository extends JpaRepository<ListingPageContent, Integer> {
    Optional<ListingPageContent> findByPageSlug(String pageSlug);

    Optional<ListingPageContent> findByPageSlugAndIsActiveTrue(String pageSlug);

    List<ListingPageContent> findAllByOrderByPageSlugAsc();
}

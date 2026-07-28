package com.mypropertyfact.estate.repositories;

import com.mypropertyfact.estate.entities.ListingPageFaq;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ListingPageFaqRepository extends JpaRepository<ListingPageFaq, Integer> {
    List<ListingPageFaq> findByPageSlugAndIsActiveTrueOrderBySortOrderAscIdAsc(String pageSlug);

    List<ListingPageFaq> findAllByOrderByPageSlugAscSortOrderAscIdAsc();
}

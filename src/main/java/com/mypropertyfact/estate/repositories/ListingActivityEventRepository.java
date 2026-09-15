package com.mypropertyfact.estate.repositories;

import com.mypropertyfact.estate.entities.ListingActivityEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ListingActivityEventRepository extends JpaRepository<ListingActivityEvent, Long> {

    List<ListingActivityEvent> findBySourceAndEntityIdOrderByOccurredAtAsc(String source, Long entityId);

    List<ListingActivityEvent> findBySourceAndEntityIdIn(String source, Collection<Long> entityIds);
}

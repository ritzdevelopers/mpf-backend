package com.mypropertyfact.estate.repositories;

import com.mypropertyfact.estate.entities.SiteTrafficEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SiteTrafficEventRepository extends JpaRepository<SiteTrafficEvent, Long> {

    long countByOccurredAtAfter(LocalDateTime since);

    long countByOccurredAtGreaterThanEqualAndOccurredAtLessThan(
            LocalDateTime fromInclusive, LocalDateTime toExclusive);

    Page<SiteTrafficEvent> findByOccurredAtAfterOrderByOccurredAtDesc(LocalDateTime since, Pageable pageable);

    @Query("SELECT e.path, COUNT(e) FROM SiteTrafficEvent e WHERE e.occurredAt >= :since GROUP BY e.path ORDER BY COUNT(e) DESC")
    List<Object[]> countGroupedByPathSince(@Param("since") LocalDateTime since, Pageable pageable);

    @Modifying
    @Query("DELETE FROM SiteTrafficEvent e WHERE e.occurredAt < :cutoff")
    int deleteByOccurredAtBefore(@Param("cutoff") LocalDateTime cutoff);
}

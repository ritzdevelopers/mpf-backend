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

    /**
     * Removes recent "ping" rows (dwell_ms IS NULL) for the same session + path so a completed
     * visit (dwell) does not double-count with an earlier real-time ping.
     */
    @Modifying
    @Query("DELETE FROM SiteTrafficEvent e WHERE e.clientSessionId = :sid AND e.path = :path AND e.dwellMs IS NULL AND e.occurredAt >= :since")
    int deletePingsForSessionPathSince(
            @Param("sid") String sid,
            @Param("path") String path,
            @Param("since") LocalDateTime since);

    @Query(
            value = "SELECT DATE_FORMAT(occurred_at, '%Y-%m-%d %H:%i:00') AS bk, COUNT(*) AS cnt "
                    + "FROM site_traffic_event WHERE occurred_at >= :since "
                    + "GROUP BY bk ORDER BY bk ASC",
            nativeQuery = true)
    List<Object[]> countGroupedBySqlMinute(@Param("since") LocalDateTime since);

    @Query(
            value = "SELECT HOUR(occurred_at) AS hr, COUNT(*) AS cnt FROM site_traffic_event "
                    + "WHERE occurred_at >= :from AND occurred_at < :to "
                    + "GROUP BY HOUR(occurred_at) ORDER BY hr",
            nativeQuery = true)
    List<Object[]> countGroupedBySqlHour(
            @Param("from") LocalDateTime fromInclusive,
            @Param("to") LocalDateTime toExclusive);
}

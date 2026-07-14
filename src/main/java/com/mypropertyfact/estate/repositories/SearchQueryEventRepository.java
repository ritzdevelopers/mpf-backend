package com.mypropertyfact.estate.repositories;

import com.mypropertyfact.estate.entities.SearchQueryEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SearchQueryEventRepository extends JpaRepository<SearchQueryEvent, Long> {

    long countByOccurredAtGreaterThanEqualAndOccurredAtLessThan(
            LocalDateTime fromInclusive, LocalDateTime toExclusive);

    long countBySearchTypeAndOccurredAtGreaterThanEqualAndOccurredAtLessThan(
            String searchType, LocalDateTime fromInclusive, LocalDateTime toExclusive);

    @Query(
            "SELECT COUNT(DISTINCT e.queryNormalized) FROM SearchQueryEvent e "
                    + "WHERE e.occurredAt >= :from AND e.occurredAt < :to")
    long countDistinctQueriesInRange(
            @Param("from") LocalDateTime fromInclusive, @Param("to") LocalDateTime toExclusive);

    @Query(
            "SELECT e.queryNormalized, e.searchType, COUNT(e), COUNT(DISTINCT e.clientSessionId), "
                    + "MAX(e.targetLabel) FROM SearchQueryEvent e "
                    + "WHERE e.occurredAt >= :from AND e.occurredAt < :to "
                    + "GROUP BY e.queryNormalized, e.searchType "
                    + "ORDER BY COUNT(e) DESC")
    List<Object[]> topQueriesGrouped(
            @Param("from") LocalDateTime fromInclusive,
            @Param("to") LocalDateTime toExclusive,
            Pageable pageable);

    @Query(
            "SELECT e.queryNormalized, e.searchType, COUNT(e), COUNT(DISTINCT e.clientSessionId), "
                    + "MAX(e.targetLabel) FROM SearchQueryEvent e "
                    + "WHERE e.occurredAt >= :from AND e.occurredAt < :to AND e.searchType = :type "
                    + "GROUP BY e.queryNormalized, e.searchType "
                    + "ORDER BY COUNT(e) DESC")
    List<Object[]> topQueriesByType(
            @Param("from") LocalDateTime fromInclusive,
            @Param("to") LocalDateTime toExclusive,
            @Param("type") String type,
            Pageable pageable);

    @Query(
            value =
                    "SELECT DATE(occurred_at) AS d, "
                            + "COUNT(*) AS total, "
                            + "SUM(CASE WHEN search_type = 'property' THEN 1 ELSE 0 END) AS property_cnt, "
                            + "SUM(CASE WHEN search_type = 'blog' THEN 1 ELSE 0 END) AS blog_cnt, "
                            + "SUM(CASE WHEN search_type = 'keyword' THEN 1 ELSE 0 END) AS keyword_cnt "
                            + "FROM search_query_event "
                            + "WHERE occurred_at >= :from AND occurred_at < :to "
                            + "GROUP BY DATE(occurred_at) ORDER BY d ASC",
            nativeQuery = true)
    List<Object[]> dailyBuckets(
            @Param("from") LocalDateTime fromInclusive, @Param("to") LocalDateTime toExclusive);

    List<SearchQueryEvent> findByOccurredAtGreaterThanEqualAndOccurredAtLessThanOrderByOccurredAtDesc(
            LocalDateTime fromInclusive, LocalDateTime toExclusive);

    @Modifying
    @Query("DELETE FROM SearchQueryEvent e WHERE e.occurredAt < :cutoff")
    int deleteByOccurredAtBefore(@Param("cutoff") LocalDateTime cutoff);

    @Query(
            "SELECT COUNT(e) FROM SearchQueryEvent e WHERE e.clientSessionId = :sid "
                    + "AND e.queryNormalized = :q AND e.searchType = :type AND e.occurredAt >= :since")
    long countRecentDuplicate(
            @Param("sid") String sid,
            @Param("q") String queryNormalized,
            @Param("type") String type,
            @Param("since") LocalDateTime since);
}

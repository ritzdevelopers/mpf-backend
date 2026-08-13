package com.mypropertyfact.estate.repositories;

import com.mypropertyfact.estate.entities.IpTrackEvent;
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
public interface IpTrackEventRepository extends JpaRepository<IpTrackEvent, Long> {

    long countByOccurredAtAfter(LocalDateTime since);

    long countByScanTrueAndOccurredAtAfter(LocalDateTime since);

    long countByScanTrue();

    @Query("SELECT COUNT(DISTINCT e.remoteAddr) FROM IpTrackEvent e")
    long countDistinctRemoteAddr();

    @Query("SELECT COUNT(DISTINCT e.remoteAddr) FROM IpTrackEvent e WHERE e.scan = true")
    long countDistinctScanRemoteAddr();

    @Modifying
    @Query("DELETE FROM IpTrackEvent e WHERE e.occurredAt < :cutoff")
    int deleteByOccurredAtBefore(@Param("cutoff") LocalDateTime cutoff);

    Page<IpTrackEvent> findByRemoteAddrOrderByOccurredAtDesc(String remoteAddr, Pageable pageable);

    Page<IpTrackEvent> findByOrderByOccurredAtDesc(Pageable pageable);

    Page<IpTrackEvent> findByScanTrueOrderByOccurredAtDesc(Pageable pageable);

    List<IpTrackEvent> findTop20ByRemoteAddrOrderByOccurredAtDesc(String remoteAddr);

    /**
     * Aggregated IP rows: hit count, scan count, first/last seen, latest geo fields.
     * Ordered by last_seen DESC.
     */
    @Query(
            value = "SELECT remote_addr AS ip, "
                    + "COUNT(*) AS hit_count, "
                    + "SUM(CASE WHEN is_scan = 1 THEN 1 ELSE 0 END) AS scan_count, "
                    + "MIN(occurred_at) AS first_seen, "
                    + "MAX(occurred_at) AS last_seen, "
                    + "SUBSTRING_INDEX(GROUP_CONCAT(country ORDER BY occurred_at DESC SEPARATOR '\\0'), '\\0', 1) AS country, "
                    + "SUBSTRING_INDEX(GROUP_CONCAT(region ORDER BY occurred_at DESC SEPARATOR '\\0'), '\\0', 1) AS region, "
                    + "SUBSTRING_INDEX(GROUP_CONCAT(city ORDER BY occurred_at DESC SEPARATOR '\\0'), '\\0', 1) AS city, "
                    + "CAST(SUBSTRING_INDEX(GROUP_CONCAT(latitude ORDER BY occurred_at DESC SEPARATOR '\\0'), '\\0', 1) AS DECIMAL(12,8)) AS latitude, "
                    + "CAST(SUBSTRING_INDEX(GROUP_CONCAT(longitude ORDER BY occurred_at DESC SEPARATOR '\\0'), '\\0', 1) AS DECIMAL(12,8)) AS longitude, "
                    + "SUBSTRING_INDEX(GROUP_CONCAT(org ORDER BY occurred_at DESC SEPARATOR '\\0'), '\\0', 1) AS org "
                    + "FROM ip_track_event "
                    + "WHERE (:scansOnly = 0 OR is_scan = 1) "
                    + "AND (:since IS NULL OR occurred_at >= :since) "
                    + "GROUP BY remote_addr "
                    + "ORDER BY last_seen DESC",
            countQuery = "SELECT COUNT(*) FROM ("
                    + "SELECT remote_addr FROM ip_track_event "
                    + "WHERE (:scansOnly = 0 OR is_scan = 1) "
                    + "AND (:since IS NULL OR occurred_at >= :since) "
                    + "GROUP BY remote_addr"
                    + ") t",
            nativeQuery = true)
    Page<Object[]> aggregateByIp(
            @Param("scansOnly") int scansOnly,
            @Param("since") LocalDateTime since,
            Pageable pageable);

    long countByRemoteAddrAndPathAndOccurredAtAfter(
            String remoteAddr, String path, LocalDateTime since);
}

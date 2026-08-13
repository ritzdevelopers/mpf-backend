package com.mypropertyfact.estate.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Per-hit log of visitors and scanners hitting the public site.
 * Aggregated in admin UI by IP for hit counts, scan counts, and geo.
 */
@Entity
@Table(
        name = "ip_track_event",
        indexes = {
                @Index(name = "idx_ip_track_occurred", columnList = "occurred_at"),
                @Index(name = "idx_ip_track_addr_occurred", columnList = "remote_addr,occurred_at"),
                @Index(name = "idx_ip_track_scan_occurred", columnList = "is_scan,occurred_at")
        })
@Getter
@Setter
@NoArgsConstructor
public class IpTrackEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreationTimestamp
    @Column(name = "occurred_at", nullable = false, updatable = false)
    private LocalDateTime occurredAt;

    @Column(name = "remote_addr", nullable = false, length = 64)
    private String remoteAddr;

    @Column(nullable = false, length = 512)
    private String path;

    @Column(name = "http_method", length = 16)
    private String httpMethod;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(length = 64)
    private String country;

    @Column(length = 64)
    private String region;

    @Column(length = 64)
    private String city;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Column(length = 255)
    private String org;

    /** True when path matches known probe / secret-scan patterns. */
    @Column(name = "is_scan", nullable = false)
    private boolean scan;

    /** middleware | beacon | api */
    @Column(length = 32)
    private String source;
}

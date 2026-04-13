package com.mypropertyfact.estate.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "site_traffic_event",
        indexes = {
                @Index(name = "idx_site_traffic_occurred", columnList = "occurred_at"),
                @Index(name = "idx_site_traffic_path_occurred", columnList = "path,occurred_at")
        })
@Getter
@Setter
@NoArgsConstructor
public class SiteTrafficEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreationTimestamp
    @Column(name = "occurred_at", nullable = false, updatable = false)
    private LocalDateTime occurredAt;

    @Column(nullable = false, length = 512)
    private String path;

    @Column(name = "client_session_id", length = 64)
    private String clientSessionId;

    @Column(name = "remote_addr", length = 64)
    private String remoteAddr;

    /** Time spent on this path (ms) before next navigation or tab close; null for legacy pings. */
    @Column(name = "dwell_ms")
    private Integer dwellMs;
}

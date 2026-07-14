package com.mypropertyfact.estate.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Records user search activity on the public site — property, blog, or free-text keyword searches.
 */
@Entity
@Table(
        name = "search_query_event",
        indexes = {
                @Index(name = "idx_search_query_occurred", columnList = "occurred_at"),
                @Index(name = "idx_search_query_type_occurred", columnList = "search_type,occurred_at"),
                @Index(name = "idx_search_query_norm_occurred", columnList = "query_normalized,occurred_at")
        })
@Getter
@Setter
@NoArgsConstructor
public class SearchQueryEvent {

    public static final String TYPE_PROPERTY = "property";
    public static final String TYPE_BLOG = "blog";
    public static final String TYPE_KEYWORD = "keyword";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreationTimestamp
    @Column(name = "occurred_at", nullable = false, updatable = false)
    private LocalDateTime occurredAt;

    /** Original query text as typed/selected by the user. */
    @Column(name = "query_text", nullable = false, length = 512)
    private String queryText;

    /** Lowercased, trimmed form used for aggregation. */
    @Column(name = "query_normalized", nullable = false, length = 512)
    private String queryNormalized;

    /** One of: property | blog | keyword */
    @Column(name = "search_type", nullable = false, length = 32)
    private String searchType;

    /** Optional project slug, blog slug, or similar target. */
    @Column(name = "target_ref", length = 255)
    private String targetRef;

    /** Optional human label for the target (project/blog title). */
    @Column(name = "target_label", length = 255)
    private String targetLabel;

    /** Optional result count returned to the user for this search. */
    @Column(name = "result_count")
    private Integer resultCount;

    /** Page where the search originated, e.g. / or /projects or /blog. */
    @Column(name = "source_path", length = 512)
    private String sourcePath;

    @Column(name = "client_session_id", length = 64)
    private String clientSessionId;

    @Column(name = "remote_addr", length = 64)
    private String remoteAddr;
}

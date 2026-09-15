package com.mypropertyfact.estate.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Append-only activity for website projects and portal listings.
 * Does not duplicate the listing itself — only records who did what, and when.
 */
@Entity
@Table(
        name = "listing_activity_event",
        indexes = {
                @Index(name = "idx_listing_activity_source_entity", columnList = "source,entity_id,occurred_at"),
                @Index(name = "idx_listing_activity_occurred", columnList = "occurred_at")
        })
@Getter
@Setter
@NoArgsConstructor
public class ListingActivityEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** {@code PROJECT} (website) or {@code PORTAL} (broker/owner listing). */
    @Column(nullable = false, length = 16)
    private String source;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    /**
     * CREATED, UPDATED, STATUS_CHANGED, APPROVED, REJECTED, PUBLISHED, UNPUBLISHED.
     */
    @Column(nullable = false, length = 32)
    private String action;

    @Column(name = "actor_user_id")
    private Integer actorUserId;

    @Column(name = "actor_name", length = 255)
    private String actorName;

    @Column(name = "detail", length = 500)
    private String detail;

    @CreationTimestamp
    @Column(name = "occurred_at", nullable = false, updatable = false)
    private LocalDateTime occurredAt;
}

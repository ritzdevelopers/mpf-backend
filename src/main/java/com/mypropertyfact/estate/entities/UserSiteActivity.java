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
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_site_activity",
        indexes = {
                @Index(name = "idx_user_activity_user_type", columnList = "user_id,activity_type,updated_at"),
                @Index(name = "idx_user_activity_lookup", columnList = "user_id,activity_type,entity_type,entity_slug")
        })
@Getter
@Setter
@NoArgsConstructor
public class UserSiteActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "activity_type", nullable = false, length = 32)
    private String activityType;

    @Column(name = "entity_type", length = 32)
    private String entityType;

    @Column(name = "entity_id", length = 64)
    private String entityId;

    @Column(name = "entity_slug", length = 255)
    private String entitySlug;

    @Column(name = "entity_label", length = 255)
    private String entityLabel;

    @Column(length = 500)
    private String href;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

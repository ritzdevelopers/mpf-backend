package com.mypropertyfact.estate.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Append-only log of admin dashboard actions for the "Your recent tasks" widget.
 */
@Entity
@Table(
        name = "admin_dashboard_activity",
        indexes = {
            @Index(name = "idx_admin_activity_actor_created", columnList = "actor_user_id,created_at")
        })
@Getter
@Setter
@NoArgsConstructor
public class AdminDashboardActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_user_id", nullable = false)
    private Integer actorUserId;

    @Column(name = "task_type", nullable = false, length = 64)
    private String taskType;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(length = 600)
    private String href;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

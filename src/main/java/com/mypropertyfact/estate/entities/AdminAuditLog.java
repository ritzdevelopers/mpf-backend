package com.mypropertyfact.estate.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "admin_audit_log",
        indexes = {
                @Index(name = "idx_admin_audit_occurred", columnList = "occurred_at"),
                @Index(name = "idx_admin_audit_actor_email", columnList = "actor_email"),
                @Index(name = "idx_admin_audit_success", columnList = "success")
        })
@Getter
@Setter
@NoArgsConstructor
public class AdminAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private LocalDateTime occurredAt;

    @Column(name = "actor_email", nullable = false, length = 255)
    private String actorEmail;

    @Column(name = "actor_user_id")
    private Integer actorUserId;

    @Column(name = "http_method", nullable = false, length = 16)
    private String httpMethod;

    @Column(name = "request_path", nullable = false, length = 1000)
    private String requestPath;

    @Column(name = "query_string", length = 256)
    private String queryString;

    @Column(name = "http_status", nullable = false)
    private int httpStatus;

    @Column(name = "success", nullable = false)
    private boolean success;

    @Column(name = "duration_ms", nullable = false)
    private int durationMs;

    /** Human-readable description of the action (not raw API). */
    @Column(name = "task_label", length = 512)
    private String taskLabel;

    /** Dashboard route the client was on when the request ran (from X-MPF-Admin-Page). */
    @Column(name = "client_admin_page", length = 512)
    private String clientAdminPage;

    /** Milliseconds the user had been on that dashboard page (from X-MPF-Dwell-Ms). */
    @Column(name = "client_dwell_ms")
    private Integer clientDwellMs;
}

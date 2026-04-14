package com.mypropertyfact.estate.entities;

import com.mypropertyfact.estate.enums.AdminPasswordResetStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "admin_password_reset_request",
        indexes = {
                @Index(name = "idx_aprr_status_created", columnList = "status,created_at"),
                @Index(name = "idx_aprr_user_status", columnList = "user_id,status")
        })
@Getter
@Setter
@NoArgsConstructor
public class AdminPasswordResetRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Bcrypt hash of the password the admin asked for (never store plaintext). */
    @Column(name = "proposed_password_hash", nullable = false, length = 255)
    private String proposedPasswordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdminPasswordResetStatus status = AdminPasswordResetStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
}

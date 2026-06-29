package com.mypropertyfact.estate.repositories;

import com.mypropertyfact.estate.entities.AdminAuditLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long>,
        JpaSpecificationExecutor<AdminAuditLog> {

    List<AdminAuditLog> findBySuccessTrueAndOccurredAtAfterOrderByOccurredAtDesc(
            LocalDateTime since,
            Pageable pageable);

    @Modifying
    @Query("DELETE FROM AdminAuditLog a WHERE a.occurredAt < :cutoff")
    int deleteByOccurredAtBefore(@Param("cutoff") LocalDateTime cutoff);
}

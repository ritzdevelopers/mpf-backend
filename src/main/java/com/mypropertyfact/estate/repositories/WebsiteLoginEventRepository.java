package com.mypropertyfact.estate.repositories;

import com.mypropertyfact.estate.entities.WebsiteLoginEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface WebsiteLoginEventRepository extends JpaRepository<WebsiteLoginEvent, Long> {

    Page<WebsiteLoginEvent> findByOrderByLoggedInAtDesc(Pageable pageable);

    long countByLoggedInAtAfter(LocalDateTime since);

    @Query("SELECT COUNT(DISTINCT e.userId) FROM WebsiteLoginEvent e WHERE e.loggedInAt > :since")
    long countDistinctUsersSince(@Param("since") LocalDateTime since);

    @Query("""
            SELECT e FROM WebsiteLoginEvent e
            WHERE (:q IS NULL OR :q = ''
                OR LOWER(COALESCE(e.fullName, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(e.email, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(e.phone, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(e.city, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(e.region, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(e.country, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(e.remoteAddr, '')) LIKE LOWER(CONCAT('%', :q, '%')))
            ORDER BY e.loggedInAt DESC
            """)
    Page<WebsiteLoginEvent> search(@Param("q") String q, Pageable pageable);
}

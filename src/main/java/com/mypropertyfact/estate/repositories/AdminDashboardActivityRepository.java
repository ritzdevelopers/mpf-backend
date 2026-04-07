package com.mypropertyfact.estate.repositories;

import com.mypropertyfact.estate.entities.AdminDashboardActivity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminDashboardActivityRepository extends JpaRepository<AdminDashboardActivity, Long> {

    List<AdminDashboardActivity> findByActorUserIdOrderByCreatedAtDesc(
            Integer actorUserId, Pageable pageable);
}

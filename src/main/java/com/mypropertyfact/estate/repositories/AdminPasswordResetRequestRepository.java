package com.mypropertyfact.estate.repositories;

import com.mypropertyfact.estate.entities.AdminPasswordResetRequest;
import com.mypropertyfact.estate.enums.AdminPasswordResetStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminPasswordResetRequestRepository extends JpaRepository<AdminPasswordResetRequest, Long> {

    List<AdminPasswordResetRequest> findByStatusOrderByCreatedAtDesc(AdminPasswordResetStatus status);

    void deleteByUser_IdAndStatus(Integer userId, AdminPasswordResetStatus status);

    long countByStatus(AdminPasswordResetStatus status);
}

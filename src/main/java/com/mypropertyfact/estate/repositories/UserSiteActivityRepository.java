package com.mypropertyfact.estate.repositories;

import com.mypropertyfact.estate.entities.UserSiteActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSiteActivityRepository extends JpaRepository<UserSiteActivity, Long> {

    long countByUserIdAndActivityType(Integer userId, String activityType);

    List<UserSiteActivity> findTop40ByUserIdOrderByUpdatedAtDesc(Integer userId);

    List<UserSiteActivity> findTop40ByUserIdAndActivityTypeOrderByUpdatedAtDesc(
            Integer userId, String activityType);

    Optional<UserSiteActivity> findFirstByUserIdAndActivityTypeAndEntityTypeAndEntitySlug(
            Integer userId, String activityType, String entityType, String entitySlug);

    void deleteByUserIdAndActivityTypeAndEntityTypeAndEntitySlug(
            Integer userId, String activityType, String entityType, String entitySlug);
}

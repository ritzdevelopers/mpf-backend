package com.mypropertyfact.estate.repositories;

import com.mypropertyfact.estate.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);

    Optional<User> findByEmailIgnoreCase(String email);
    Optional<User> findByPhone(String phone);

    Optional<User> findByDashboardUsername(String dashboardUsername);

    List<User> findByFullNameContainingIgnoreCase(String fullName);

    @Query("""
            SELECT COUNT(DISTINCT u.id) FROM User u JOIN u.roles r
            WHERE UPPER(r.roleName) = UPPER(:roleName) AND r.isActive = true
            """)
    long countDistinctUsersHavingActiveRole(@Param("roleName") String roleName);

    @Query("""
            SELECT u FROM User u
            WHERE u.adminStaffApproved = false
            ORDER BY u.createdAt ASC
            """)
    List<User> findPendingPortalApprovals();
}

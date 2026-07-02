package com.mypropertyfact.estate.repositories;

import com.mypropertyfact.estate.entities.ProjectWalkthrough;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectWalkthroughRepository extends JpaRepository<ProjectWalkthrough, Integer> {
    Optional<ProjectWalkthrough> findFirstByProject_IdOrderByIdDesc(int projectId);

    /**
     * Flat SQL read avoids Hibernate OneToOne inverse-mapping failures when production
     * has duplicate project_id rows in project_walkthrough.
     */
    @Query(value = """
            SELECT pw.id, pw.walkthrough_desc, pw.project_id, p.project_name
            FROM project_walkthrough pw
            LEFT JOIN projects p ON p.id = pw.project_id
            WHERE pw.id IN (
                SELECT MAX(inner_pw.id) FROM project_walkthrough inner_pw GROUP BY inner_pw.project_id
            )
            ORDER BY pw.id
            """, nativeQuery = true)
    List<Object[]> findAllSummaries();

    @Query(value = """
            SELECT pw.id, pw.walkthrough_desc, pw.walkthrough_image, pw.project_id, p.project_name
            FROM project_walkthrough pw
            LEFT JOIN projects p ON p.id = pw.project_id
            WHERE pw.id = :id
            """, nativeQuery = true)
    List<Object[]> findDetailsById(@Param("id") int id);
}

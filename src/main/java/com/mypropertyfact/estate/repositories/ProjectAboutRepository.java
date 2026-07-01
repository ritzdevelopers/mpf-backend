package com.mypropertyfact.estate.repositories;

import com.mypropertyfact.estate.entities.ProjectsAbout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectAboutRepository extends JpaRepository<ProjectsAbout, Integer> {
    Optional<ProjectsAbout> findByProject_Id(int projectId);

    /**
     * Flat SQL read avoids Hibernate OneToOne inverse-mapping failures when production
     * has duplicate project_id rows in projects_about.
     */
    @Query(value = """
            SELECT pa.id, pa.short_desc, pa.long_desc, pa.project_id, p.project_name
            FROM projects_about pa
            LEFT JOIN projects p ON p.id = pa.project_id
            WHERE pa.id IN (
                SELECT MAX(inner_pa.id) FROM projects_about inner_pa GROUP BY inner_pa.project_id
            )
            ORDER BY pa.id
            """, nativeQuery = true)
    List<Object[]> findAllSummaries();
}

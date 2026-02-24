package com.mypropertyfact.estate.repositories;

import com.mypropertyfact.estate.dtos.FloorPlanDto;
import com.mypropertyfact.estate.entities.FloorPlan;
import com.mypropertyfact.estate.entities.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FloorPlanRepository extends JpaRepository<FloorPlan, Integer> {

    List<FloorPlan> findByProject(Project project);

    /**
     * Efficient query that fetches only the required fields for floor plans with project info
     * Returns: [floorPlanId, planType, areaSqft, areaSqMt, projectId, projectName]
     * Note: Removed ORDER BY to improve query performance - sorting can be done in Java if needed
     */
    @Query(value = """
             SELECT 
                fp.id,
                fp.plan_type,
                fp.area_sqft,
                fp.area_sqmt,
                p.id,
                p.project_name
            FROM projects p
            LEFT JOIN floor_plans fp ON fp.project_id = p.id
            """, nativeQuery = true)
    List<Object[]> findAllFloorPlansWithProjectInfo();

    @Query("""
            SELECT new com.mypropertyfact.estate.dtos.FloorPlanDto(
            fp.planType,
            fp.areaSqft,
            fp.areaSqmt
            )
            FROM FloorPlan fp
            WHERE fp.project.id = :projectId
            """)
    List<FloorPlanDto> findByProjectId(@Param("projectId") int projectId);
}

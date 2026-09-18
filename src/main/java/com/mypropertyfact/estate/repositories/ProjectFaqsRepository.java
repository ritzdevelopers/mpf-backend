package com.mypropertyfact.estate.repositories;

import com.mypropertyfact.estate.dtos.ProjectFaqDto;
import com.mypropertyfact.estate.entities.Project;
import com.mypropertyfact.estate.entities.ProjectFaqs;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ProjectFaqsRepository extends JpaRepository<ProjectFaqs, Integer> {
    @Query("SELECT fq.faqQuestion as question, " +
            "fq.faqAnswer as answer, fq.id FROM " +
            "ProjectFaqs fq")
    List<Object[]> getAllWithProjectName();

    List<ProjectFaqs> findBySlugUrl(String url);

    List<ProjectFaqs> findByProject(Project project);

    @Query("""
            SELECT new com.mypropertyfact.estate.dtos.ProjectFaqDto(
            pf.id,
            pf.project.id,
            pf.faqQuestion,
            pf.faqAnswer
            )
            FROM ProjectFaqs pf
            WHERE pf.project.id = :projectId
            """)
    List<ProjectFaqDto> findByProjectId(@Param("projectId") int projectId);

    @Query(
            value = """
            SELECT p.id, p.projectName, COUNT(pf)
            FROM ProjectFaqs pf
            JOIN pf.project p
            GROUP BY p.id, p.projectName
            ORDER BY p.projectName ASC
            """,
            countQuery = """
            SELECT COUNT(DISTINCT p.id)
            FROM ProjectFaqs pf
            JOIN pf.project p
            """
    )
    Page<Object[]> findProjectFaqSummaries(Pageable pageable);

    @Query("""
            SELECT pf FROM ProjectFaqs pf
            JOIN FETCH pf.project p
            WHERE p.id IN :projectIds
            ORDER BY p.projectName ASC, pf.id ASC
            """)
    List<ProjectFaqs> findByProjectIds(@Param("projectIds") Collection<Integer> projectIds);
}

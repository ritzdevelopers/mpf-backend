package com.mypropertyfact.estate.repositories;

import com.mypropertyfact.estate.dtos.ProjectFaqDto;
import com.mypropertyfact.estate.entities.Project;
import com.mypropertyfact.estate.entities.ProjectFaqs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}

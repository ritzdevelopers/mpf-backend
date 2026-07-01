package com.mypropertyfact.estate.services;

import com.mypropertyfact.estate.dtos.ProjectWalkthroughDto;
import com.mypropertyfact.estate.entities.Project;
import com.mypropertyfact.estate.entities.ProjectWalkthrough;
import com.mypropertyfact.estate.models.Response;
import com.mypropertyfact.estate.repositories.ProjectRepository;
import com.mypropertyfact.estate.repositories.ProjectWalkthroughRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProjectWalkthroughService {

    private final ProjectWalkthroughRepository projectWalkthroughRepository;

    private final ProjectRepository projectRepository;

    public List<ProjectWalkthroughDto> getAllWalkthrough() {
        return projectWalkthroughRepository.findAllSummaries().stream()
                .map(this::mapSummaryRow)
                .toList();
    }

    public ProjectWalkthroughDto getWalkthroughById(int id) {
        return projectWalkthroughRepository.findDetailsById(id)
                .map(this::mapDetailsRow)
                .orElse(null);
    }

    private ProjectWalkthroughDto mapSummaryRow(Object[] row) {
        ProjectWalkthroughDto projectWalkthroughDto = new ProjectWalkthroughDto();
        projectWalkthroughDto.setId(((Number) row[0]).intValue());
        projectWalkthroughDto.setWalkthroughDesc(buildWalkthroughPreview((String) row[1]));
        if (row[2] != null) {
            projectWalkthroughDto.setProjectId(((Number) row[2]).intValue());
        }
        projectWalkthroughDto.setProjectName((String) row[3]);
        return projectWalkthroughDto;
    }

    private ProjectWalkthroughDto mapDetailsRow(Object[] row) {
        ProjectWalkthroughDto projectWalkthroughDto = new ProjectWalkthroughDto();
        projectWalkthroughDto.setId(((Number) row[0]).intValue());
        projectWalkthroughDto.setWalkthroughDesc((String) row[1]);
        projectWalkthroughDto.setWalkthroughImage((String) row[2]);
        if (row[3] != null) {
            projectWalkthroughDto.setProjectId(((Number) row[3]).intValue());
        }
        projectWalkthroughDto.setProjectName((String) row[4]);
        return projectWalkthroughDto;
    }

    private String buildWalkthroughPreview(String walkthroughDesc) {
        if (walkthroughDesc == null || walkthroughDesc.isBlank()) {
            return "";
        }
        String plainText = walkthroughDesc.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
        if (plainText.length() <= 120) {
            return plainText;
        }
        return plainText.substring(0, 120) + "...";
    }

    public Response addUpdate(ProjectWalkthroughDto projectWalkthroughDto) {
        Response response = new Response();
        try {
            if (projectWalkthroughDto.getWalkthroughDesc() == null
                    || projectWalkthroughDto.getWalkthroughDesc().isBlank()) {
                response.setMessage("All fields are required !");
                return response;
            }
            if (projectWalkthroughDto.getProjectId() <= 0) {
                response.setMessage("Please select a project.");
                return response;
            }
            Optional<Project> project = projectRepository.findById(projectWalkthroughDto.getProjectId());
            if (project.isEmpty()) {
                response.setMessage("Selected project was not found.");
                return response;
            }
            if (projectWalkthroughDto.getId() > 0) {
                Optional<ProjectWalkthrough> savedWalkthrough =
                        projectWalkthroughRepository.findById(projectWalkthroughDto.getId());
                if (savedWalkthrough.isEmpty()) {
                    response.setMessage("Walkthrough entry was not found.");
                    return response;
                }
                ProjectWalkthrough walkthrough = savedWalkthrough.get();
                walkthrough.setWalkthroughDesc(projectWalkthroughDto.getWalkthroughDesc());
                walkthrough.setProject(project.get());
                projectWalkthroughRepository.save(walkthrough);
                response.setMessage("Walkthrough updated successfully...");
                response.setIsSuccess(1);
            } else {
                Optional<ProjectWalkthrough> existingForProject = projectWalkthroughRepository
                        .findFirstByProject_IdOrderByIdDesc(projectWalkthroughDto.getProjectId());
                if (existingForProject.isPresent()) {
                    response.setMessage("This project already has a walkthrough. Please update the existing entry.");
                    response.setIsSuccess(0);
                    return response;
                }
                ProjectWalkthrough projectWalkthrough = new ProjectWalkthrough();
                projectWalkthrough.setWalkthroughDesc(projectWalkthroughDto.getWalkthroughDesc());
                projectWalkthrough.setProject(project.get());
                projectWalkthroughRepository.save(projectWalkthrough);
                response.setMessage("Walkthrough saved successfully...");
                response.setIsSuccess(1);
            }
        } catch (DataIntegrityViolationException e) {
            response.setMessage("This project already has a walkthrough. Please update the existing entry.");
            response.setIsSuccess(0);
        } catch (Exception e) {
            response.setMessage(e.getMessage() != null ? e.getMessage() : "Failed to save walkthrough.");
        }
        return response;
    }

    public Response deleteWalkthrough(int id) {
        this.projectWalkthroughRepository.deleteById(id);
        return new Response(1, "Deleted", 0);
    }
}

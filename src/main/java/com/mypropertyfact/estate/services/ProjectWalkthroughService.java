package com.mypropertyfact.estate.services;

import com.mypropertyfact.estate.dtos.ProjectWalkthroughDto;
import com.mypropertyfact.estate.entities.Project;
import com.mypropertyfact.estate.entities.ProjectWalkthrough;
import com.mypropertyfact.estate.models.Response;
import com.mypropertyfact.estate.repositories.ProjectRepository;
import com.mypropertyfact.estate.repositories.ProjectWalkthroughRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProjectWalkthroughService {

    private final ProjectWalkthroughRepository projectWalkthroughRepository;

    private final ProjectRepository projectRepository;

    public List<ProjectWalkthroughDto> getAllWalkthrough() {
        List<ProjectWalkthrough> allWalkthrough = this.projectWalkthroughRepository.findAll();
        return allWalkthrough.stream().map(walkthrough-> {
            ProjectWalkthroughDto projectWalkthroughDto = new ProjectWalkthroughDto();
            projectWalkthroughDto.setId(walkthrough.getId());
            if (walkthrough.getProject() != null) {
                projectWalkthroughDto.setProjectId(walkthrough.getProject().getId());
                projectWalkthroughDto.setProjectName(walkthrough.getProject().getProjectName());
            }
            projectWalkthroughDto.setWalkthroughDesc(buildWalkthroughPreview(walkthrough.getWalkthroughDesc()));
            return projectWalkthroughDto;
        }).toList();
    }

    public ProjectWalkthroughDto getWalkthroughById(int id) {
        return projectWalkthroughRepository.findById(id)
                .map(this::mapToDto)
                .orElse(null);
    }

    private ProjectWalkthroughDto mapToDto(ProjectWalkthrough walkthrough) {
        ProjectWalkthroughDto projectWalkthroughDto = new ProjectWalkthroughDto();
        projectWalkthroughDto.setId(walkthrough.getId());
        if (walkthrough.getProject() != null) {
            projectWalkthroughDto.setProjectId(walkthrough.getProject().getId());
            projectWalkthroughDto.setProjectName(walkthrough.getProject().getProjectName());
        }
        projectWalkthroughDto.setWalkthroughDesc(walkthrough.getWalkthroughDesc());
        projectWalkthroughDto.setWalkthroughImage(walkthrough.getWalkthroughImage());
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
            Optional<Project> project = projectRepository.findById(projectWalkthroughDto.getProjectId());
            if (projectWalkthroughDto.getId() > 0) {
                Optional<ProjectWalkthrough> savedWalkthrough = projectWalkthroughRepository.findById(projectWalkthroughDto.getId());
                savedWalkthrough.ifPresent(walkthrough -> {
                    walkthrough.setWalkthroughDesc(projectWalkthroughDto.getWalkthroughDesc());
                    project.ifPresent(walkthrough::setProject);
                    projectWalkthroughRepository.save(walkthrough);
                    response.setMessage("Walkthrough updated successfully...");
                    response.setIsSuccess(1);
                });
            } else {
                ProjectWalkthrough projectWalkthrough = new ProjectWalkthrough();
                projectWalkthrough.setWalkthroughDesc(projectWalkthroughDto.getWalkthroughDesc());
                project.ifPresent(projectWalkthrough::setProject);
                projectWalkthroughRepository.save(projectWalkthrough);
                response.setMessage("Walkthrough saved successfully...");
                response.setIsSuccess(1);
            }
        } catch (Exception e) {
            response.setMessage(e.getMessage());
        }
        return response;
    }

    public Response deleteWalkthrough(int id) {
        this.projectWalkthroughRepository.deleteById(id);
        return new Response(1, "Deleted", 0);
    }
}

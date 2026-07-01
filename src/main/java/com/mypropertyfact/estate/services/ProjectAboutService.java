package com.mypropertyfact.estate.services;

import com.mypropertyfact.estate.Constants;
import com.mypropertyfact.estate.dtos.ProjectAboutDto;
import com.mypropertyfact.estate.entities.Project;
import com.mypropertyfact.estate.entities.ProjectsAbout;
import com.mypropertyfact.estate.models.Response;
import com.mypropertyfact.estate.repositories.ProjectAboutRepository;
import com.mypropertyfact.estate.repositories.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProjectAboutService {

    private final ProjectAboutRepository projectAboutRepository;

    private final ProjectRepository projectRepository;

    public List<Map<String, Object>> getAllProjectsAbout(){
        return projectAboutRepository.findAllSummaries().stream().map(row -> {
            Map<String, Object> response = new HashMap<>();
            response.put("id", row[0]);
            response.put("shortDesc", row[1]);
            response.put("longDesc", row[2]);
            response.put("projectId", row[3]);
            response.put("projectName", row[4]);
            return response;
        }).toList();
    }

    public Response addUpdate(ProjectAboutDto projectAboutDto){
        Response response = new Response();
        try{
            String shortDesc = projectAboutDto.getShortDesc();
            String longDesc = projectAboutDto.getLongDesc();
            if (shortDesc == null || shortDesc.isBlank() || longDesc == null || longDesc.isBlank()) {
                response.setMessage(Constants.ALL_FIELDS_REQUIRED);
                return response;
            }
            if (projectAboutDto.getProjectId() <= 0) {
                response.setMessage("Please select a project.");
                return response;
            }
            if (!projectRepository.existsById(projectAboutDto.getProjectId())) {
                response.setMessage("Selected project was not found.");
                return response;
            }
            Project projectRef = projectRepository.getReferenceById(projectAboutDto.getProjectId());
            if(projectAboutDto.getId() > 0){
                Optional<ProjectsAbout> saveData = projectAboutRepository.findById(projectAboutDto.getId());
                if (saveData.isEmpty()) {
                    response.setMessage("Project about entry was not found.");
                    return response;
                }
                ProjectsAbout about = saveData.get();
                about.setShortDesc(shortDesc);
                about.setLongDesc(longDesc);
                about.setProject(projectRef);
                projectAboutRepository.save(about);
                response.setMessage("Project's about details updated successfully...");
                response.setIsSuccess(1);
            }else{
                Optional<ProjectsAbout> existingForProject =
                        projectAboutRepository.findFirstByProject_IdOrderByIdDesc(projectAboutDto.getProjectId());
                if (existingForProject.isPresent()) {
                    response.setMessage("This project already has 'about' details. Please update the existing entry.");
                    response.setIsSuccess(0);
                    return response;
                }
                ProjectsAbout projectAbout = new ProjectsAbout();
                projectAbout.setLongDesc(longDesc);
                projectAbout.setShortDesc(shortDesc);
                projectAbout.setProject(projectRef);
                projectAboutRepository.save(projectAbout);
                response.setMessage("Project's about details saved successfully...");
                response.setIsSuccess(1);
            }
        } catch (DataIntegrityViolationException e) {
            response.setMessage("This project already has 'about' details. Please update the existing entry.");
            response.setIsSuccess(0);
        } catch (Exception e){
            response.setMessage(e.getMessage() != null ? e.getMessage() : "Failed to save project about.");
        }
        return response;
    }

    public Response deleteProjectsAbout(int id){
        
        this.projectAboutRepository.deleteById(id);
        return new Response(1,"Data deleted successfully...", 0);
    }
//    public ProjectsAbout getBySlug(String url){
//        Project projectBySlugURL = projectRepository.findBySlugURL(url);
//        if(projectBySlugURL != null){
//
//        }
//        return projectAboutRepository.findBySlugURL(url);
//    }
}

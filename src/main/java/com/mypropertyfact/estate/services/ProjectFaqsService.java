package com.mypropertyfact.estate.services;
import com.mypropertyfact.estate.dtos.ProjectFaqDto;
import com.mypropertyfact.estate.dtos.ProjectFaqGroupDto;
import com.mypropertyfact.estate.dtos.ProjectFaqItemDto;
import com.mypropertyfact.estate.dtos.ProjectFaqPageResponse;
import com.mypropertyfact.estate.entities.Project;
import com.mypropertyfact.estate.entities.ProjectFaqs;
import com.mypropertyfact.estate.models.Response;
import com.mypropertyfact.estate.repositories.ProjectFaqsRepository;
import com.mypropertyfact.estate.repositories.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ProjectFaqsService {

    private final ProjectFaqsRepository projectFaqsRepository;

    private final ProjectRepository projectRepository;

    public ProjectFaqPageResponse getAllFaqs(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(safePage, safeSize);
        Page<Object[]> summaries = projectFaqsRepository.findProjectFaqSummaries(pageable);

        if (summaries.isEmpty()) {
            return ProjectFaqPageResponse.builder()
                    .content(List.of())
                    .totalElements(summaries.getTotalElements())
                    .totalPages(summaries.getTotalPages())
                    .number(summaries.getNumber())
                    .size(summaries.getSize())
                    .build();
        }

        List<Integer> projectIds = summaries.getContent().stream()
                .map(row -> (Integer) row[0])
                .toList();
        List<ProjectFaqs> faqs = projectFaqsRepository.findByProjectIds(projectIds);
        Map<Integer, List<ProjectFaqItemDto>> faqsByProjectId = new LinkedHashMap<>();
        for (ProjectFaqs projectFaq : faqs) {
            int projectId = projectFaq.getProject().getId();
            faqsByProjectId
                    .computeIfAbsent(projectId, id -> new ArrayList<>())
                    .add(new ProjectFaqItemDto(
                            projectFaq.getId(),
                            projectFaq.getFaqQuestion(),
                            projectFaq.getFaqAnswer()
                    ));
        }

        List<ProjectFaqGroupDto> content = summaries.getContent().stream()
                .map(row -> {
                    int projectId = (Integer) row[0];
                    String projectName = (String) row[1];
                    long faqCount = row[2] instanceof Number number ? number.longValue() : 0L;
                    List<ProjectFaqItemDto> projectFaq = faqsByProjectId.getOrDefault(projectId, List.of());
                    return ProjectFaqGroupDto.builder()
                            .projectId(projectId)
                            .projectName(projectName)
                            .noOfFaqs((int) faqCount)
                            .projectFaq(projectFaq)
                            .build();
                })
                .toList();

        return ProjectFaqPageResponse.builder()
                .content(content)
                .totalElements(summaries.getTotalElements())
                .totalPages(summaries.getTotalPages())
                .number(summaries.getNumber())
                .size(summaries.getSize())
                .build();
    }

    public Response addUpdateFaqs(ProjectFaqDto projectFaqDto) {
        Response response = new Response();
        try {
            if (projectFaqDto == null || projectFaqDto.getQuestion().isEmpty() || projectFaqDto.getAnswer().isEmpty()) {
                response.setMessage("All fields are required !");
                return response;
            }
            Optional<Project> project = projectRepository.findById(projectFaqDto.getProjectId());

            if (projectFaqDto.getId() > 0) {
                Optional<ProjectFaqs> savedFaqs = projectFaqsRepository.findById(projectFaqDto.getId());
                savedFaqs.ifPresent(faqs -> {
                            faqs.setFaqQuestion(projectFaqDto.getQuestion());
                            faqs.setFaqAnswer(projectFaqDto.getAnswer());
                            project.ifPresent(faqs::setProject);
                            projectFaqsRepository.save(faqs);
                            response.setMessage("Faqs updated successfully...");
                            response.setIsSuccess(1);
                        }
                );
            } else {
                ProjectFaqs projectFaqs = new ProjectFaqs();
                project.ifPresent(projectFaqs::setProject);
                projectFaqs.setFaqQuestion(projectFaqDto.getQuestion());
                projectFaqs.setFaqAnswer(projectFaqDto.getAnswer());
                this.projectFaqsRepository.save(projectFaqs);
                response.setIsSuccess(1);
                response.setMessage("Faqs added successfully...");
            }
        } catch (Exception e) {
            response.setMessage(e.getMessage());
        }
        return response;
    }

    public List<ProjectFaqs> getBySlug(String url) {
        List<ProjectFaqs> response;
        try {
            response = this.projectFaqsRepository.findBySlugUrl(url);
        } catch (Exception e) {
            response = new ArrayList<>();
        }
        return response;
    }

    public Response deleteFaq(int id) {
        Response response = new Response();
        try {
            Optional<ProjectFaqs> byId = projectFaqsRepository.findById(id);
            if (byId.isPresent()) {
                projectFaqsRepository.deleteById(id);
                response.setMessage("FAQ deleted successfully...");
                response.setIsSuccess(1);
            } else {
                response.setMessage("FAQ already deleted or not exists");
            }
        } catch (Exception e) {
            response.setMessage(e.getMessage());
        }
        return response;
    }
}

package com.mypropertyfact.estate.services;

import com.mypropertyfact.estate.dtos.BuilderResponse;
import com.mypropertyfact.estate.dtos.BuilderDto;
import com.mypropertyfact.estate.dtos.BuilderWriteDto;
import com.mypropertyfact.estate.dtos.ProjectShortDetails;
import com.mypropertyfact.estate.entities.Builder;
import com.mypropertyfact.estate.models.Response;
import com.mypropertyfact.estate.repositories.BuilderRepository;
import com.mypropertyfact.estate.repositories.ProjectRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BuilderService {

    private final BuilderRepository builderRepository;
    private final ProjectRepository projectRepository;

    // Getting all builders
    public BuilderResponse getAllBuilders() {
        return new BuilderResponse(builderRepository.findAllProjectedBy(Sort.by(Sort.Direction.ASC, "builderName")),
                new Response(1, "All builders fetched successfully...", 0));
    }

    public Response addUpdateBuilder(BuilderWriteDto dto) {
        Response response = new Response();
        try {
            if (dto == null
                    || dto.getBuilderName() == null
                    || dto.getBuilderName().isBlank()) {
                return new Response(0, "Builder name is required !", 0);
            }
            String trimmedName = dto.getBuilderName().trim();
            Builder existsBuilder = this.builderRepository.findByBuilderName(trimmedName);
            if (existsBuilder != null && existsBuilder.getId() != dto.getId()) {
                response.setMessage("Builder already exists!");
                response.setIsSuccess(0);
                return response;
            }
            String finalSlug = slugFromBuilderName(trimmedName);
            if (dto.getId() != 0) {
                Optional<Builder> existing = this.builderRepository.findById(dto.getId());
                if (existing.isEmpty()) {
                    return new Response(0, "Builder not found for update.", 0);
                }
                Builder savedBuilder = existing.get();
                savedBuilder.setBuilderName(trimmedName);
                savedBuilder.setSlugUrl(finalSlug);
                savedBuilder.setBuilderDesc(dto.getBuilderDesc());
                savedBuilder.setMetaTitle(dto.getMetaTitle());
                savedBuilder.setMetaKeyword(dto.getMetaKeyword());
                savedBuilder.setMetaDesc(dto.getMetaDesc());
                this.builderRepository.save(savedBuilder);
                response.setIsSuccess(1);
                response.setMessage("Builder Updated Successfully...");
            } else {
                Builder builder = new Builder();
                builder.setBuilderName(trimmedName);
                builder.setSlugUrl(finalSlug);
                builder.setBuilderDesc(dto.getBuilderDesc());
                builder.setMetaTitle(dto.getMetaTitle());
                builder.setMetaKeyword(dto.getMetaKeyword());
                builder.setMetaDesc(dto.getMetaDesc());
                this.builderRepository.save(builder);
                response.setMessage("Builder saved successfully...");
                response.setIsSuccess(1);
            }
        } catch (Exception e) {
            response.setMessage(e.getMessage() != null ? e.getMessage() : "Could not save builder.");
            response.setIsSuccess(0);
        }
        return response;
    }

    private static String slugFromBuilderName(String builderName) {
        String slugUrl = builderName.toLowerCase(Locale.ROOT);
        String[] words = slugUrl.split("\\s+");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (words[i].isEmpty()) {
                continue;
            }
            if (result.length() > 0) {
                result.append("-");
            }
            result.append(words[i]);
        }
        return result.toString();
    }

    public Response deleteBuilder(int id) {
        Response response = new Response();
        try {
            this.builderRepository.deleteById(id);
            response.setIsSuccess(1);
            response.setMessage("Builder Deleted Successfully...");
        } catch (Exception e) {
            response.setMessage(e.getMessage());
            response.setIsSuccess(0);
        }
        return response;
    }

    @Transactional
    public BuilderDto getBySlug(String url) {
        Optional<Builder> dbBuilder = this.builderRepository.findBySlugUrl(url);
        List<ProjectShortDetails> projectDetailDtoList = projectRepository.findAllProjects().stream()
                .filter(project -> project.getBuilderSlug()
                        .equals(url))
                .collect(Collectors.toList());
        BuilderDto builderDto = new BuilderDto();
        dbBuilder.ifPresent(builder -> {
            builderDto.setId(builder.getId());
            builderDto.setBuilderName(builder.getBuilderName());
            ;
            builderDto.setBuilderDescription(builder.getBuilderDesc());
            builderDto.setMetaTitle(builder.getMetaTitle());
            builderDto.setMetaKeywords(builder.getMetaKeyword());
            builderDto.setMetaDescription(builder.getMetaDesc());
            builderDto.setProjectList(projectDetailDtoList);
        });
        return builderDto;
    }

    public List<Builder> getAllBuildersList() {
        return builderRepository.findAll();
    }
}

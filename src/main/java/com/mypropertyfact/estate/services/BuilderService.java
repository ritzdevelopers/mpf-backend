package com.mypropertyfact.estate.services;

import com.mypropertyfact.estate.common.CommonMapper;
import com.mypropertyfact.estate.dtos.BuilderResponse;
import com.mypropertyfact.estate.dtos.BuilderDto;
import com.mypropertyfact.estate.dtos.ProjectDetailDto;
import com.mypropertyfact.estate.dtos.ProjectShortDetails;
import com.mypropertyfact.estate.entities.Builder;
import com.mypropertyfact.estate.entities.Project;
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

    private final CommonMapper commonMapper;

    // Getting all builders
    public BuilderResponse getAllBuilders() {
        return new BuilderResponse(builderRepository.findAllProjectedBy(Sort.by(Sort.Direction.ASC, "builderName")),
                new Response(1, "All builders fetched successfully...", 0));
    }

    public Response addUpdateBuilder(Builder builder) {
        Response response = new Response();
        try {
            if (builder == null || builder.getBuilderName().isEmpty()) {
                return new Response(0, "Builder name is required !", 0);
            }
            Builder existsBuilder = this.builderRepository.findByBuilderName(builder.getBuilderName());
            if (existsBuilder != null && existsBuilder.getId() != builder.getId()) {
                response.setMessage("Builder already exists!");
                response.setIsSuccess(0);
                return response;
            }
            String slugUrl = builder.getBuilderName(); // Convert to lowercase
            String[] words = slugUrl.toLowerCase().split(" "); // Split the string by spaces
            StringBuilder result = new StringBuilder();

            // Iterate over the words
            for (int i = 0; i < words.length; i++) {
                result.append(words[i]); // Add the current word to the result
                // If it's not the last word, add a hyphen
                if (i < words.length - 1) {
                    result.append("-");
                }
            }
            // The result is the slug URL
            String finalSlug = result.toString();
            builder.setSlugUrl(finalSlug);
            if (builder.getId() != 0) {
                Builder savedBuilder = this.builderRepository.findById(builder.getId()).get();
                savedBuilder.setBuilderName(builder.getBuilderName());
                savedBuilder.setSlugUrl(builder.getSlugUrl());
                savedBuilder.setBuilderDesc(builder.getBuilderDesc());
                savedBuilder.setMetaTitle(builder.getMetaTitle());
                savedBuilder.setMetaKeyword(builder.getMetaKeyword());
                savedBuilder.setMetaDesc(builder.getMetaDesc());
                this.builderRepository.save(savedBuilder);
                response.setIsSuccess(1);
                response.setMessage("Builder Updated Successfully...");
            } else {
                this.builderRepository.save(builder);
                response.setMessage("Builder saved successfully...");
                response.setIsSuccess(1);
            }
        } catch (Exception e) {
            response.setMessage(e.getMessage());
            response.setIsSuccess(0);
        }
        return response;
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
                .filter(project -> project.getBuilderName()
                        .trim().toLowerCase()
                        .equals(url.toLowerCase().trim()))
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

package com.mypropertyfact.estate.services;

import com.mypropertyfact.estate.common.FileUtils;
import com.mypropertyfact.estate.dtos.WebStoryCategoryDto;
import com.mypropertyfact.estate.dtos.WebStoryDto;
import com.mypropertyfact.estate.entities.WebStory;
import com.mypropertyfact.estate.entities.WebStoryCategory;
import com.mypropertyfact.estate.interfaces.WebStoryCategoryService;
import com.mypropertyfact.estate.models.Response;
import com.mypropertyfact.estate.repositories.WebStoryCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class WebStoryCategoryServiceImpl implements WebStoryCategoryService {

    private final WebStoryCategoryRepository webStoryCategoryRepository;

    private final FileUtils fileUtils;

    private final AdminDashboardActivityService adminDashboardActivityService;

    @Value("${upload_dir}")
    private String uploadDir;

    @Override
    public Response addUpdate(WebStoryCategoryDto webStoryCategoryDto) {
        Response response = new Response();

        String slug = fileUtils.generateSlug(webStoryCategoryDto.getCategoryName());

        // Check for duplicate category name
        Optional<WebStoryCategory> existingCategory = webStoryCategoryRepository.findByCategoryName(slug);

        if (webStoryCategoryDto.getId() > 0) {
            Optional<WebStoryCategory> savedCategory = webStoryCategoryRepository.findById(webStoryCategoryDto.getId());

            if (savedCategory.isPresent()) {
                WebStoryCategory category = savedCategory.get();

                // Prevent updating to a name that already exists in another record
                if (existingCategory.isPresent() && !(existingCategory.get().getId() == category.getId())) {
                    response.setIsSuccess(0);
                    response.setMessage("Category name already exists.");
                    return response;
                }

                category.setCategoryName(slug);
                category.setCategoryDescription(webStoryCategoryDto.getCategoryDescription());
                category.setMetaDescription(webStoryCategoryDto.getMetaDescription());
                category.setMetaKeywords(webStoryCategoryDto.getMetaKeywords());
                webStoryCategoryRepository.save(category);
                adminDashboardActivityService.recordForCurrentUser(
                        AdminDashboardActivityService.TASK_WEB_STORY_CATEGORY,
                        "Updated web story category: " + category.getCategoryName(),
                        "/admin/dashboard/web-story-category");
                response.setIsSuccess(1);
                response.setMessage("Web story category updated successfully...");
            } else {
                response.setIsSuccess(0);
                response.setMessage("Web story category not found.");
            }

        } else {
            // New insert - check if name already exists
            if (existingCategory.isPresent()) {
                response.setIsSuccess(0);
                response.setMessage("Category name already exists.");
                return response;
            }

            WebStoryCategory webStoryCategory = new WebStoryCategory();
            webStoryCategory.setCategoryName(slug);
            webStoryCategory.setCategoryDescription(webStoryCategoryDto.getCategoryDescription());
            webStoryCategory.setMetaDescription(webStoryCategoryDto.getMetaDescription());
            webStoryCategory.setMetaKeywords(webStoryCategoryDto.getMetaKeywords());
            webStoryCategoryRepository.save(webStoryCategory);
            adminDashboardActivityService.recordForCurrentUser(
                    AdminDashboardActivityService.TASK_WEB_STORY_CATEGORY,
                    "Added web story category: " + webStoryCategory.getCategoryName(),
                    "/admin/dashboard/web-story-category");
            response.setIsSuccess(1);
            response.setMessage("Web story category saved successfully...");
        }

        return response;
    }


    @Override
    @Transactional
    public List<WebStoryCategoryDto> getAllCategories() {
        List<WebStoryCategory> allCategories = webStoryCategoryRepository.findAll();
        return allCategories.stream().map(category -> {
            WebStoryCategoryDto webStoryCategoryDto = new WebStoryCategoryDto();
            webStoryCategoryDto.setId(category.getId());
            webStoryCategoryDto.setCategoryName(category.getCategoryName());
            webStoryCategoryDto.setCategoryDescription(category.getCategoryDescription());
            webStoryCategoryDto.setMetaDescription(category.getMetaDescription());
            webStoryCategoryDto.setMetaKeywords(category.getMetaKeywords());
            if(category.getWebStories() != null && !category.getWebStories().isEmpty()) {
                webStoryCategoryDto.setStoryCategoryImage(category.getWebStories().get(0).getStoryImage());
            }
            assert category.getWebStories() != null;
            List<WebStoryDto> webStoryDtoList = category.getWebStories().stream().map(webStory -> {
                WebStoryDto webStoryDto = new WebStoryDto();
                webStoryDto.setCategoryId(webStory.getWebStoryCategory().getId());
                webStoryDto.setStoryImage(webStory.getStoryImage());
                webStoryDto.setStoryTitle(webStoryDto.getStoryTitle());
                webStoryDto.setStoryDescription(webStory.getStoryDescription());
                webStoryDto.setId(webStory.getId());
                return webStoryDto;
            }).toList();
            webStoryCategoryDto.setWebStories(webStoryDtoList);
            return webStoryCategoryDto;
        }).toList();
    }
    @Transactional
    @Override
    public Response deleteCategory(int categoryId) {
        try{
            Optional<WebStoryCategory> webStoryCategory = webStoryCategoryRepository.findById(categoryId);
            String storyDestination = uploadDir.concat("/web-story");
            webStoryCategory.ifPresent(category-> {
                List<WebStory> webStories = category.getWebStories();
                if(webStories != null){
                    webStories.forEach(story-> {
                        String image = story.getStoryImage();
                        if(image != null){
                            fileUtils.deleteFileFromDestination(story.getStoryImage(), storyDestination);
                        }
                    });
                }
            });
            webStoryCategoryRepository.deleteById(categoryId);
            return new Response(1, "Category deleted with it's all stories", 0);
        }catch (Exception e){
            return new Response(0, e.getMessage(), 0);
        }
    }
}

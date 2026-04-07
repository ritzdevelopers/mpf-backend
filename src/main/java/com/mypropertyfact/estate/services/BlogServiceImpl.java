package com.mypropertyfact.estate.services;

import com.mypropertyfact.estate.common.FileUtils;
import com.mypropertyfact.estate.dtos.BlogDto;
import com.mypropertyfact.estate.entities.Blog;
import com.mypropertyfact.estate.entities.BlogCategory;
import com.mypropertyfact.estate.entities.City;
import com.mypropertyfact.estate.interfaces.BlogService;
import com.mypropertyfact.estate.models.ResourceNotFoundException;
import com.mypropertyfact.estate.models.Response;
import com.mypropertyfact.estate.repositories.BlogCategoryRepository;
import com.mypropertyfact.estate.repositories.BlogRepository;
import com.mypropertyfact.estate.repositories.CityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class BlogServiceImpl implements BlogService {

    private final BlogRepository blogRepository;

    private final BlogCategoryRepository blogCategoryRepository;

    private final CityRepository cityRepository;

    @Value("${upload_dir}")
    private String upload_dir;

    private final FileUtils fileUtils;

    private final AdminDashboardActivityService adminDashboardActivityService;

    @Override
    public Response addUpdateBlog(MultipartFile blogImage, BlogDto blogDto) {
        // Generate slug first
        String generatedSlug = fileUtils.generateSlug(blogDto.getSlugUrl());
        blogDto.setSlugUrl(generatedSlug);
        Optional<BlogCategory> blogCategory = blogCategoryRepository.findById(Integer.parseInt(blogDto.getBlogCategory()));
        Optional<City> city = cityRepository.findById(blogDto.getCityId());
        String blogImageName = null;
        Blog existing = blogDto.getId() > 0 ? blogRepository.findById(blogDto.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Blog not found")) : new Blog();
        if (blogImage != null && !blogImage.isEmpty()) {
            // Validate file
            if (!fileUtils.isFileSizeValid(blogImage, 5 * 1024 * 1024)) {
                throw new IllegalArgumentException("File size exceeds the 2MB limit.");
            }
            if (!fileUtils.isTypeImage(blogImage)) {
                throw new IllegalArgumentException("Invalid file type.");
            }
            // Delete old image BEFORE saving new one
            if (blogDto.getId() > 0) {
                existing = blogRepository.findById(blogDto.getId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Blog not found"));

                if (existing.getBlogImage() != null && !existing.getBlogImage().isBlank()) {
                    fileUtils.deleteFileFromDestination(existing.getBlogImage(), upload_dir + "blogDto/");
                }
            }
            // Rename and save image
            String imageName = fileUtils.renameFile(blogImage, blogDto.getSlugUrl());
            String dir = Paths.get(upload_dir, "blog/").toString();
            blogImageName = fileUtils.saveFile(blogImage, imageName, dir, 1200, 628, 0.9f); // Save resized and converted
        }

        // Update blogDto
        if (blogDto.getId() > 0) {
            // Delete old image only if new one is uploaded
            if (blogImageName != null && existing.getBlogImage() != null) {
                fileUtils.deleteFileFromDestination(existing.getBlogImage(), upload_dir + "blogDto/");
            }

            // Update fields
            existing.setBlogTitle(blogDto.getBlogTitle());
            existing.setBlogDescription(blogDto.getBlogDescription());
            if(!existing.getSlugUrl().equals(blogDto.getSlugUrl())){
                existing.setSlugUrl(blogDto.getSlugUrl());
            }
            existing.setStatus(blogDto.getStatus());
            existing.setBlogKeywords(blogDto.getBlogKeywords());
            blogCategory.ifPresent(existing::setBlogCategory);
            city.ifPresent(existing::setCity);
            existing.setBlogMetaDescription(blogDto.getBlogMetaDescription());
            if (blogImageName != null) {
                existing.setBlogImage(blogImageName);
            }
            blogRepository.save(existing);
            adminDashboardActivityService.recordForCurrentUser(
                    AdminDashboardActivityService.TASK_BLOG,
                    "Updated blog: " + blogDto.getBlogTitle(),
                    "/admin/dashboard/manage-blogs");
            return new Response(1, "Blog updated successfully...", 0);
        }

        // Add new blogDto
        if (blogRepository.existsBySlugUrl(blogDto.getSlugUrl())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Slug URL already exists");
        }
        Blog blog = new Blog();
        blogCategory.ifPresent(blog::setBlogCategory);
        city.ifPresent(blog::setCity);
        blog.setBlogTitle(blogDto.getBlogTitle());
        blog.setBlogDescription(blogDto.getBlogDescription());
        blog.setSlugUrl(blogDto.getSlugUrl());
        blog.setStatus(blogDto.getStatus());
        blog.setBlogKeywords(blogDto.getBlogKeywords());
        blog.setBlogMetaDescription(blogDto.getBlogMetaDescription());
        blog.setBlogImage(blogImageName);
        blogRepository.save(blog);
        adminDashboardActivityService.recordForCurrentUser(
                AdminDashboardActivityService.TASK_BLOG,
                "Added blog: " + blogDto.getBlogTitle(),
                "/admin/dashboard/manage-blogs");
        return new Response(1, "Blog saved successfully...", 0);
    }

    @Override
    public Optional<Blog> getBlogById(int id) {
        if (!blogRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Blog not found");
        }
        return blogRepository.findById(id);
    }

    @Override
    public List<BlogDto> getAllBlogs() {
        List<Blog> blogs = blogRepository.findAllWithBlogCategory();
        return blogs.stream().map(blog ->
                new BlogDto(blog.getId(),
                        blog.getBlogTitle(),
                        blog.getBlogKeywords(),
                        blog.getBlogMetaDescription(),
                        blog.getBlogDescription(),
                        blog.getSlugUrl(),
                        blog.getBlogImage(),
                        blog.getBlogCategory() != null ? blog.getBlogCategory().getCategoryName(): null,
                        blog.getStatus(),
                        blog.getBlogCategory() != null ?blog.getBlogCategory().getId() : 0,
                        blog.getCity() != null ? blog.getCity().getId(): 0,
                        blog.getCity() != null ? blog.getCity().getName(): null,
                        blog.getCreatedAt()
                )
        ).collect(Collectors.toList());
    }

    public BlogDto getBySlug(String slug) {
        Blog bySlugUrl = blogRepository.findBySlugUrl(slug);
        return new BlogDto(bySlugUrl.getId(),
                bySlugUrl.getBlogTitle(),
                bySlugUrl.getBlogKeywords(),
                bySlugUrl.getBlogMetaDescription(),
                bySlugUrl.getBlogDescription(),
                bySlugUrl.getSlugUrl(),
                bySlugUrl.getBlogImage(),
                bySlugUrl.getBlogCategory() != null ? bySlugUrl.getBlogCategory().getCategoryName(): null,
                bySlugUrl.getStatus(),
                bySlugUrl.getBlogCategory() != null ? bySlugUrl.getBlogCategory().getId(): 0,
                bySlugUrl.getCity() != null ? bySlugUrl.getCity().getId(): 0,
                bySlugUrl.getCity() != null ? bySlugUrl.getCity().getName(): null,
                bySlugUrl.getCreatedAt()
        );
    }

    @Override
    public Page<BlogDto> getWithPagination(int page, int size, String from, String search) {
        // Step 1: Fetch all blogs (no pagination)
        List<Blog> allBlogs = blogRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));

        // Step 2: Map to DTOs
        List<BlogDto> dtoList = allBlogs.stream().map(blog -> {
            BlogDto blogDto = new BlogDto();
            blogDto.setBlogImage(blog.getBlogImage());
            blogDto.setCityName(blog.getCity() != null ? blog.getCity().getName() : null);
            blogDto.setCityId(blog.getCity() != null ? blog.getCity().getId() : 0);
            blogDto.setBlogCategory(blog.getBlogCategory() != null ? blog.getBlogCategory().getCategoryName() : null);
            blogDto.setCategoryId(blog.getBlogCategory() != null ? blog.getBlogCategory().getId() : 0);
            blogDto.setSlugUrl(blog.getSlugUrl());
            blogDto.setCreatedAt(blog.getCreatedAt());
            blogDto.setBlogTitle(blog.getBlogTitle());
            blogDto.setBlogMetaDescription(blog.getBlogMetaDescription());
            return blogDto;
        }).toList();

        // Step 3: Filter based on "from" and "search"
        List<BlogDto> filteredList;
        if ("blog".equalsIgnoreCase(from)) {
            filteredList = dtoList.stream()
                    .filter(blog -> blog.getCategoryId() != 5)
                    .toList();
        } else {
            filteredList = dtoList.stream()
                    .filter(blog -> blog.getCategoryId() == 5)
                    .toList();
        }
        // Step 4: Manual pagination on the filtered list
        int start = Math.min(page * size, filteredList.size());
        int end = Math.min(start + size, filteredList.size());
        List<BlogDto> pagedList = filteredList.subList(start, end);
        if (search != null && !search.isEmpty()) {
            return new PageImpl<>(filteredList, PageRequest.of(page, size), filteredList.size());
        }

        // Step 5: Return as Page<BlogDto>
        return new PageImpl<>(pagedList, PageRequest.of(page, size), filteredList.size());
    }



    @Override
    public Response deleteBlog(int id) {
        Blog blog = blogRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Blog already deleted or not found"));
        fileUtils.deleteFileFromDestination(blog.getBlogImage(), upload_dir + "blog/");
        blogRepository.delete(blog);
        return new Response(1, "Blog deleted successful...", 0);
    }
}

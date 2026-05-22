package com.mypropertyfact.estate.services;

import com.mypropertyfact.estate.backup.MpfBackupExcelWriter;
import com.mypropertyfact.estate.dtos.ProjectExportDto;
import com.mypropertyfact.estate.entities.*;
import com.mypropertyfact.estate.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MpfBackupExcelExportService {

    private final ProjectService projectService;
    private final EnqueryRepository enqueryRepository;
    private final BlogRepository blogRepository;
    private final PropertyListingRepository propertyListingRepository;
    private final BuilderRepository builderRepository;
    private final WebStoryRepository webStoryRepository;
    private final UserRepository userRepository;
    private final HomeBannerRepository homeBannerRepository;
    private final AmenityRepository amenityRepository;

    @Transactional(readOnly = true)
    public List<String> exportAllExcelFiles(Path excelDir) throws Exception {
        Files.createDirectories(excelDir);
        List<String> written = new ArrayList<>();

        writeSafe(excelDir.resolve("properties.xlsx"), () -> {
            List<ProjectExportDto> projects = projectService.findAllForExcelExport();
            MpfBackupExcelWriter.writeDtoWorkbook(
                    excelDir.resolve("properties.xlsx"), "Properties", projects, ProjectExportDto.class);
        }, written, "properties.xlsx");

        writeSafe(excelDir.resolve("enquiries.xlsx"), () -> {
            MpfBackupExcelWriter.writeDtoWorkbook(
                    excelDir.resolve("enquiries.xlsx"),
                    "Enquiries",
                    enqueryRepository.findAll(),
                    Enquery.class);
        }, written, "enquiries.xlsx");

        writeSafe(excelDir.resolve("blogs.xlsx"), () -> {
            List<Blog> blogs = blogRepository.findAll();
            MpfBackupExcelWriter.writeDtoWorkbook(
                    excelDir.resolve("blogs.xlsx"), "Blogs", blogs, Blog.class);
            exportBlogOverflowText(excelDir, blogs);
        }, written, "blogs.xlsx");

        writeSafe(excelDir.resolve("property_listings.xlsx"), () -> {
            MpfBackupExcelWriter.writeDtoWorkbook(
                    excelDir.resolve("property_listings.xlsx"),
                    "PropertyListings",
                    propertyListingRepository.findAll(),
                    PropertyListing.class);
        }, written, "property_listings.xlsx");

        writeSafe(excelDir.resolve("builders.xlsx"), () -> {
            MpfBackupExcelWriter.writeDtoWorkbook(
                    excelDir.resolve("builders.xlsx"), "Builders", builderRepository.findAll(), Builder.class);
        }, written, "builders.xlsx");

        writeSafe(excelDir.resolve("web_stories.xlsx"), () -> {
            MpfBackupExcelWriter.writeDtoWorkbook(
                    excelDir.resolve("web_stories.xlsx"),
                    "WebStories",
                    webStoryRepository.findAll(),
                    WebStory.class);
        }, written, "web_stories.xlsx");

        writeSafe(excelDir.resolve("users.xlsx"), () -> {
            List<Map<String, Object>> rows = new ArrayList<>();
            for (User u : userRepository.findAll()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", u.getId());
                row.put("fullName", u.getFullName());
                row.put("email", u.getEmail());
                row.put("dashboardUsername", u.getDashboardUsername());
                row.put("phone", u.getPhone());
                row.put("location", u.getLocation());
                row.put("verified", u.getVerified());
                row.put("createdAt", u.getCreatedAt());
                row.put("updatedAt", u.getUpdatedAt());
                rows.add(row);
            }
            List<String> headers = List.of(
                    "id", "fullName", "email", "dashboardUsername", "phone", "location", "verified", "createdAt", "updatedAt");
            List<List<Object>> data = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                List<Object> line = new ArrayList<>();
                for (String h : headers) {
                    line.add(row.get(h));
                }
                data.add(line);
            }
            MpfBackupExcelWriter.writeWorkbook(excelDir.resolve("users.xlsx"), "Users", headers, data);
        }, written, "users.xlsx");

        writeSafe(excelDir.resolve("home_banners.xlsx"), () -> {
            MpfBackupExcelWriter.writeDtoWorkbook(
                    excelDir.resolve("home_banners.xlsx"),
                    "HomeBanners",
                    homeBannerRepository.findAll(),
                    HomeBanner.class);
        }, written, "home_banners.xlsx");

        writeSafe(excelDir.resolve("amenities.xlsx"), () -> {
            MpfBackupExcelWriter.writeDtoWorkbook(
                    excelDir.resolve("amenities.xlsx"), "Amenities", amenityRepository.findAll(), Amenity.class);
        }, written, "amenities.xlsx");

        return written;
    }

    /** Full LONGTEXT fields that exceed Excel's cell limit are stored here for restore/reference. */
    private static void exportBlogOverflowText(Path excelDir, List<Blog> blogs) throws Exception {
        Path overflowDir = excelDir.resolve("blogs_full_text");
        int filesWritten = 0;
        for (Blog blog : blogs) {
            int id = blog.getId();
            filesWritten +=
                    writeBlogOverflowFile(overflowDir, id, "blog_description", blog.getBlogDescription());
            filesWritten += writeBlogOverflowFile(overflowDir, id, "blog_keywords", blog.getBlogKeywords());
            filesWritten +=
                    writeBlogOverflowFile(
                            overflowDir, id, "blog_meta_description", blog.getBlogMetaDescription());
        }
        if (filesWritten > 0) {
            log.info("Wrote {} blog full-text overflow file(s) under {}", filesWritten, overflowDir);
        }
    }

    private static int writeBlogOverflowFile(Path overflowDir, int blogId, String fieldName, String content)
            throws Exception {
        if (!MpfBackupExcelWriter.exceedsExcelCellLimit(content)) {
            return 0;
        }
        Path file = overflowDir.resolve("blog_" + blogId + "_" + fieldName + ".txt");
        Files.createDirectories(overflowDir);
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return 1;
    }

    private static void writeSafe(Path path, ThrowingRunnable task, List<String> written, String label)
            throws Exception {
        try {
            task.run();
            written.add(label);
        } catch (Exception e) {
            log.warn("Excel export skipped for {}: {}", label, e.getMessage());
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}

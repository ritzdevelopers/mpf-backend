package com.mypropertyfact.estate.services;

import com.mypropertyfact.estate.dtos.UserPropertySubmissionDto;
import com.mypropertyfact.estate.entities.*;
import com.mypropertyfact.estate.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserPropertyService {

    private final ProjectRepository projectRepository;

    private final CityRepository cityRepository;

    private final BuilderRepository builderRepository;

    private final ProjectTypeRepository projectTypeRepository;

    private final AmenityRepository amenityRepository;

    private final ListingActivityService listingActivityService;
    
    @Value("${upload_dir}")
    private String uploadDir;
    
    @Transactional
    public Project submitProperty(UserPropertySubmissionDto dto, MultipartFile[] images, User user) {
        log.info("Submitting property for user: {}", user.getEmail());
        
        // Create new Project
        Project project = new Project();
        
        // Map basic information
        project.setProjectName(dto.getTitle() != null ? dto.getTitle() : 
            generateTitle(dto.getBedrooms(), dto.getSubType(), dto.getLocality()));
        project.setProjectLocality(dto.getLocality());
        
        // Set configuration (e.g., "3 BHK")
        if (dto.getBedrooms() != null) {
            project.setProjectConfiguration(dto.getBedrooms() + " BHK");
        }
        
        // Set price - format without scientific notation
        if (dto.getTotalPrice() != null) {
            project.setProjectPrice(formatPriceToString(dto.getTotalPrice()));
        }
        
        // Generate slug URL
        project.setSlugURL(generateSlug(dto.getProjectName() != null ? dto.getProjectName() : 
            generateTitle(dto.getBedrooms(), dto.getSubType(), dto.getLocality())));
        
        // Initially inactive until approved
        project.setStatus(false);
        // Map description
        project.setLocationDesc(dto.getDescription());
        
        // Map location/builder - Find or create City
        if (dto.getCity() != null) {
            City city = cityRepository.findByNameIgnoreCase(dto.getCity())
                .orElseGet(() -> {
                    log.info("Creating new city: {}", dto.getCity());
                    City newCity = new City();
                    newCity.setName(dto.getCity());
                    newCity.setSlugUrl(generateSlug(dto.getCity()));
                    newCity.setCreatedAt(LocalDateTime.now());
                    newCity.setUpdatedAt(LocalDateTime.now());
                    return cityRepository.save(newCity);
                });
            project.setCity(city);
        }
        
        // Find or create Builder
        if (dto.getBuilderName() != null && !dto.getBuilderName().isEmpty()) {
            Builder builder = builderRepository.findByBuilderNameIgnoreCase(dto.getBuilderName())
                .orElseGet(() -> {
                    log.info("Creating new builder: {}", dto.getBuilderName());
                    Builder newBuilder = new Builder();
                    newBuilder.setBuilderName(dto.getBuilderName());
                    newBuilder.setSlugUrl(generateSlug(dto.getBuilderName()));
                    newBuilder.setCreatedAt(LocalDateTime.now());
                    newBuilder.setUpdatedAt(LocalDateTime.now());
                    return builderRepository.save(newBuilder);
                });
            project.setBuilder(builder);
        }
        
        // Find Project Type (property subtype)
        if (dto.getSubType() != null) {
            ProjectTypes projectType = projectTypeRepository.findByProjectTypeNameIgnoreCase(dto.getSubType())
                .orElse(null);
            project.setProjectTypes(projectType);
        }
        
        // Save uploaded images - save to properties/{slug}/ folder like regular gallery images
        Set<ProjectGallery> galleries = new HashSet<>();
        if (images != null && images.length > 0) {
            log.info("Processing {} images", images.length);
            
            // Generate slug if not already set
            if (project.getSlugURL() == null || project.getSlugURL().isEmpty()) {
                String slug = generateSlug(project.getProjectName());
                project.setSlugURL(slug);
            }
            
            // Save to properties/{slug}/ folder (same as regular gallery images)
            String galleryDir = uploadDir + project.getSlugURL() + File.separator;
            File destinationDir = new File(galleryDir);
            if (!destinationDir.exists()) {
                boolean created = destinationDir.mkdirs();
                if(!created) {
                    throw new RuntimeException("Unable to create directory.");
                }
            }
            
            for (int i = 0; i < images.length; i++) {
                try {
                    MultipartFile imageFile = images[i];
                    
                    // Generate unique filename
                    String originalFilename = imageFile.getOriginalFilename();
                    String fileExtension = "";
                    if (originalFilename != null && originalFilename.contains(".")) {
                        fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
                    }
                    String fileName = System.currentTimeMillis() + (i > 0 ? "_" + i : "") + fileExtension;
                    
                    // Save image
                    File destinationFile = new File(destinationDir, fileName);
                    imageFile.transferTo(destinationFile);
                    
                    // Store only filename (not full path) in database - same as ProjectGalleryService
                    ProjectGallery gallery = new ProjectGallery();
                    gallery.setImage(fileName);  // Just the filename
                    gallery.setSlugUrl(project.getSlugURL());  // Store slug URL
                    gallery.setProject(project);
                    
                    // First image is thumbnail (store just filename)
                    if (i == 0) {
                        project.setProjectThumbnail(fileName);
                    }
                    
                    galleries.add(gallery);
                    log.info("Saved image {} to {}", fileName, galleryDir);
                } catch (Exception e) {
                    log.error("Error uploading image: {}", e.getMessage(), e);
                }
            }
            project.setProjectGalleries(galleries);
        }
        
        // Set amenities if provided - handle both IDs and names
        if (dto.getAmenities() != null && !dto.getAmenities().isEmpty()) {
            Set<Amenity> amenitySet = new HashSet<>();
            for (Object amenityValue : dto.getAmenities()) {
                if (amenityValue instanceof Integer) {
                    // It's an ID
                    amenityRepository.findById((Integer) amenityValue).ifPresent(amenitySet::add);
                } else if (amenityValue instanceof String amenityName) {
                    // It's a name - find or create amenity by name
                    Optional<Amenity> existingAmenity = amenityRepository.findByTitleIgnoreCase(amenityName);
                    
                    if (existingAmenity.isPresent()) {
                        amenitySet.add(existingAmenity.get());
                    } else {
                        // Create new amenity if it doesn't exist
                        log.info("Creating new amenity: {}", amenityName);
                        Amenity newAmenity = new Amenity();
                        newAmenity.setTitle(amenityName);
                        newAmenity.setAltTag(amenityName.toLowerCase().replace(" ", "-"));
                        newAmenity.setStatus(true);
                        Amenity savedAmenity = amenityRepository.save(newAmenity);
                        amenitySet.add(savedAmenity);
                    }
                }
            }
            project.setAmenities(amenitySet);
        }
        // Set timestamps
        project.setCreatedAt(LocalDateTime.now());
        project.setUpdatedAt(LocalDateTime.now());
        if (user != null) {
            project.setCreatedBy(user);
        }

        // Save project
        Project savedProject = projectRepository.save(project);
        listingActivityService.record(
                ListingActivityService.SOURCE_PROJECT,
                (long) savedProject.getId(),
                ListingActivityService.ACTION_CREATED,
                user,
                savedProject.getProjectName());
        log.info("Property saved successfully with ID: {}", savedProject.getId());
        
        return savedProject;
    }
    
    @Transactional
    public Project submitForApproval(int projectId, int userId) {
        log.info("Submitting property {} for approval by user {}", projectId, userId);
        
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new RuntimeException("Project not found"));
        return projectRepository.save(project);
    }

    private String generateTitle(Integer bedrooms, String subType, String locality) {
        if (bedrooms != null && subType != null && locality != null) {
            return bedrooms + " BHK " + subType + " in " + locality;
        }
        return "Property";
    }
    
    private String generateSlug(String text) {
        if (text == null) {
            return UUID.randomUUID().toString();
        }
        return text.toLowerCase()
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("^-|-$", "")
            + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
    
    /**
     * Format price as string without scientific notation
     * Converts 1.5E7 to "15000000"
     */
    private String formatPriceToString(Double price) {
        if (price == null) {
            return null;
        }
        // Use BigDecimal to avoid scientific notation
        BigDecimal bd = BigDecimal.valueOf(price);
        // Remove trailing zeros and decimal point if not needed
        bd = bd.setScale(0, RoundingMode.HALF_UP);
        return bd.toPlainString();
    }
}


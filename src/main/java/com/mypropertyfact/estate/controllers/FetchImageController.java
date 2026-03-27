package com.mypropertyfact.estate.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping({"/api/v1/get/images", "/api/v1/fetch-image"})
@RequiredArgsConstructor
public class FetchImageController {

    @Value("${uploads_path}")
    private String uploadDir;

    @Value("${upload_amenity_path}")
    private String amenityPath;

    @Value("${upload_icon_path}")
    private String iconPath;

    @Value("${upload_dir}")
    private String filePath;

    @GetMapping("/properties/{projectname}/{filename}")
    public ResponseEntity<Resource> getImage(@PathVariable String filename, @PathVariable String projectname) {
        try {
            Path imagePath = Paths.get(uploadDir, projectname + "/" + filename);
            Resource resource = new UrlResource(imagePath.toUri());

            if (resource.exists()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG) // or determine the correct type dynamically
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/amenity/{filename}")
    public ResponseEntity<Resource> getAmenityImage(@PathVariable String filename) {
        try {
            Path imagePath = Paths.get(amenityPath, filename);
            Resource resource = new UrlResource(imagePath.toUri());

            if (resource.exists()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG) // or determine the correct type dynamically
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/feature/{filename}")
    public ResponseEntity<Resource> getFeatureImage(@PathVariable String filename) {
        try {
            Path imagePath = Paths.get(filePath + "feature/", filename);
            Resource resource = new UrlResource(imagePath.toUri());

            if (resource.exists()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/icon/{filename}")
    public ResponseEntity<Resource> getIcon(@PathVariable String filename) {
        try {
            Path imagePath = Paths.get(iconPath, filename);
            Resource resource = new UrlResource(imagePath.toUri());

            if (resource.exists()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG) // or determine the correct type dynamically
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{blogFolder}/{filename}")
    public ResponseEntity<Resource> getBlogImage(@PathVariable String blogFolder, @PathVariable String filename) {
        try {
            Path imagePath = Paths.get(filePath + blogFolder, filename);
            Resource resource = new UrlResource(imagePath.toUri());

            if (resource.exists()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG) // or determine the correct type dynamically
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{blogFolder}/{contentFolder}/{filename}")
    public ResponseEntity<Resource> getBlogContentImage(@PathVariable String blogFolder,
                                                        @PathVariable String contentFolder,
                                                        @PathVariable String filename) {
        try {
            Path imagePath = Paths.get(filePath + blogFolder + "/" + contentFolder, filename);
            Resource resource = new UrlResource(imagePath.toUri());

            if (resource.exists()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG) // or determine the correct type dynamically
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/resume/{filename}")
    public ResponseEntity<Resource> getResumePdf(@PathVariable String filename) {
        try {
            Path filePathResolved = Paths.get(filePath + "resume/", filename);
            Resource resource = new UrlResource(filePathResolved.toUri());

            if (resource.exists()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_PDF)
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/web-story/{filename}")
    public ResponseEntity<Resource> getWebStoryImage(@PathVariable String filename) {
        try {
            Path imagePath = Paths.get(filePath,  "web-story/" + filename);
            Resource resource = new UrlResource(imagePath.toUri());

            if (resource.exists()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG) // or determine the correct type dynamically
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/project-gallery/{filename}")
    public ResponseEntity<Resource> getProjectGalleryImage(@PathVariable String filename) {
        try {
            // User-submitted images are stored in project-gallery folder
            Path imagePath = Paths.get(uploadDir, "project-gallery", filename);
            Resource resource = new UrlResource(imagePath.toUri());

            if (resource.exists()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/property-listings/{listingId}/{filename}")
    public ResponseEntity<Resource> getPropertyListingImage(@PathVariable String listingId, @PathVariable String filename) {
        try {
            // Property listing images are stored in: upload_dir/property-listings/{listingId}/{filename}
            Path imagePath = Paths.get(filePath + "property-listings" + "/" + listingId, filename);
            Resource resource = new UrlResource(imagePath.toUri());

            if (resource.exists()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/location-benefit/{filename}")
    public ResponseEntity<Resource> getLocationBenefitImage(@PathVariable String filename) {
        try {
            Path imagePath = Paths.get(iconPath, filename);
            Resource resource = new UrlResource(imagePath.toUri());

            if (resource.exists()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/nearby-benefit/{filename}")
    public ResponseEntity<Resource> getNearbyBenefitImage(@PathVariable String filename) {
        try {
            Path imagePath = Paths.get(filePath + "nearby-benefit/", filename);
            Resource resource = new UrlResource(imagePath.toUri());

            if (resource.exists()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

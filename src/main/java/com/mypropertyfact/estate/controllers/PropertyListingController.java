package com.mypropertyfact.estate.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mypropertyfact.estate.dtos.PropertyListingDto;
import com.mypropertyfact.estate.dtos.PropertyListingRequestDto;
import com.mypropertyfact.estate.entities.PropertyListing;
import com.mypropertyfact.estate.entities.User;
import com.mypropertyfact.estate.enums.ProjectApprovalStatus;
import com.mypropertyfact.estate.services.PropertyListingService;
import com.mypropertyfact.estate.services.UserRoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller for Property Listing operations
 * Handles user-submitted property listings from the portal form
 */
@RestController
@RequestMapping("/api/v1/user/property-listings")
@Slf4j
@RequiredArgsConstructor
public class PropertyListingController {
    
    private final PropertyListingService propertyListingService;
    private final ObjectMapper objectMapper;
    private final UserRoleService userRoleService;
    
    /**
     * Check if current user is a super admin
     */
    private boolean isSuperAdmin(User user) {
        try {
            return userRoleService.userHasRole(user.getId(), "SUPERADMIN");
        } catch (Exception e) {
            log.warn("Error checking admin role: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Create a new property listing
     * POST /api/user/property-listings
     */
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<?> createPropertyListing(
            @RequestPart(value = "images", required = false) MultipartFile[] images,
            @RequestPart("property") String propertyJson) {
        
        try {
            log.info("Received property listing creation request");
            
            // Parse JSON string to DTO
            PropertyListingRequestDto propertyDto = objectMapper.readValue(propertyJson, PropertyListingRequestDto.class);
            
            // Get current authenticated user
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = (User) auth.getPrincipal();
            
            // Create property listing
            PropertyListing savedListing = propertyListingService.createPropertyListing(
                propertyDto, images, currentUser);
            
            // Convert to DTO for response
            PropertyListingDto listingDto = propertyListingService.getPropertyListingById(savedListing.getId())
                .orElseThrow(() -> new RuntimeException("Failed to retrieve created listing"));
            
            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Property listing created successfully");
            response.put("property", listingDto);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            log.error("Error creating property listing: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to create property listing: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Get all property listings for current user
     * GET /api/user/property-listings
     */
    @GetMapping
    public ResponseEntity<?> getUserPropertyListings() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = (User) auth.getPrincipal();
            
            List<PropertyListingDto> listings = propertyListingService.getUserPropertyListings(currentUser.getId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("count", listings.size());
            response.put("properties", listings);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error fetching user property listings: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to fetch property listings: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Get property listing by ID
     * GET /api/user/property-listings/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getPropertyListing(@PathVariable Long id) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = (User) auth.getPrincipal();
            
            Optional<PropertyListingDto> listingDto = propertyListingService.getPropertyListingById(id);
            
            if (listingDto.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Property listing not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }
            
            // Verify ownership (admin can access any listing)
            if (!isSuperAdmin(currentUser) && !listingDto.get().getUserId().equals(currentUser.getId())) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Unauthorized: You don't own this property listing");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("property", listingDto.get());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error fetching property listing: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to fetch property listing: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Update property listing
     * PUT /api/user/property-listings/{id}
     */
    @RequestMapping(
            value = "/{id}",
            method = {RequestMethod.PUT, RequestMethod.PATCH},
            consumes = "multipart/form-data")
    public ResponseEntity<?> updatePropertyListing(
            @PathVariable Long id,
            @RequestPart(value = "images", required = false) MultipartFile[] images,
            @RequestPart("property") String propertyJson) {
        
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = (User) auth.getPrincipal();
            
            // Parse JSON string to DTO
            PropertyListingRequestDto propertyDto = objectMapper.readValue(propertyJson, PropertyListingRequestDto.class);
            
            // Update property listing
            PropertyListing updatedListing = propertyListingService.updatePropertyListing(
                id, propertyDto, images, currentUser);
            
            // Convert to DTO for response
            PropertyListingDto listingDto = propertyListingService.getPropertyListingById(updatedListing.getId())
                .orElseThrow(() -> new RuntimeException("Failed to retrieve updated listing"));
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Property listing updated successfully");
            response.put("property", listingDto);
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            log.error("Error updating property listing: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            
            HttpStatus status = e.getMessage().contains("not found") || e.getMessage().contains("Unauthorized")
                ? HttpStatus.NOT_FOUND : HttpStatus.INTERNAL_SERVER_ERROR;
            
            return ResponseEntity.status(status).body(errorResponse);
        } catch (Exception e) {
            log.error("Error updating property listing: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to update property listing: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Delete property listing
     * DELETE /api/user/property-listings/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePropertyListing(@PathVariable Long id) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = (User) auth.getPrincipal();
            
            propertyListingService.deletePropertyListing(id, currentUser);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Property listing deleted successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            log.error("Error deleting property listing: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        } catch (Exception e) {
            log.error("Error deleting property listing: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to delete property listing: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Get property listings by approval status
     * GET /api/user/property-listings/status/{status}
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<?> getPropertyListingsByStatus(@PathVariable String status) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = (User) auth.getPrincipal();
            
            ProjectApprovalStatus approvalStatus = ProjectApprovalStatus.valueOf(status.toUpperCase());
            List<PropertyListingDto> listings = propertyListingService.getPropertyListingsByStatus(approvalStatus);
            
            // Filter to only user's listings
            List<PropertyListingDto> userListings = listings.stream()
                .filter(listing -> listing.getUserId().equals(currentUser.getId()))
                .toList();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("status", status);
            response.put("count", userListings.size());
            response.put("properties", userListings);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Invalid status: " + status);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            log.error("Error fetching property listings by status: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to fetch property listings: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}



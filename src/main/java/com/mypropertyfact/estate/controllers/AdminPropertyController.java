package com.mypropertyfact.estate.controllers;

import com.mypropertyfact.estate.dtos.PropertyListingDto;
import com.mypropertyfact.estate.entities.PropertyListing;
import com.mypropertyfact.estate.entities.User;
import com.mypropertyfact.estate.enums.ProjectApprovalStatus;
import com.mypropertyfact.estate.services.PropertyListingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin endpoints for user-submitted property listings.
 * Access: Super Admin, or Admin with MANAGE_PROPERTY_APPROVALS (see AdminPermissionKeys).
 */
@RestController
@RequestMapping("/api/v1/admin/property-listings")
@Slf4j
@RequiredArgsConstructor
public class AdminPropertyController {
    private final PropertyListingService propertyListingService;

    @GetMapping
    @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_PROPERTY_APPROVALS')")
    public ResponseEntity<?> getAllPropertyListings(
            @RequestParam(required = false) ProjectApprovalStatus status) {
        try {
            List<PropertyListingDto> listings;

            if (status != null) {
                listings = propertyListingService.getPropertyListingsByStatus(status);
            } else {
                List<PropertyListingDto> pending =
                        propertyListingService.getPropertyListingsByStatus(
                                ProjectApprovalStatus.PENDING);
                List<PropertyListingDto> approved =
                        propertyListingService.getPropertyListingsByStatus(
                                ProjectApprovalStatus.APPROVED);
                List<PropertyListingDto> rejected =
                        propertyListingService.getPropertyListingsByStatus(
                                ProjectApprovalStatus.REJECTED);
                List<PropertyListingDto> draft =
                        propertyListingService.getPropertyListingsByStatus(
                                ProjectApprovalStatus.DRAFT);

                listings = new java.util.ArrayList<>();
                listings.addAll(pending);
                listings.addAll(approved);
                listings.addAll(rejected);
                listings.addAll(draft);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("count", listings.size());
            response.put("properties", listings);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching all property listings: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put(
                    "message", "Failed to fetch property listings: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Must be registered before {@code GET /{id}} so "pending" is not captured as a path variable.
     */
    @GetMapping("/pending")
    @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_PROPERTY_APPROVALS')")
    public ResponseEntity<?> getPendingPropertyListings() {
        try {
            List<PropertyListingDto> listings =
                    propertyListingService.getPropertyListingsByStatus(
                            ProjectApprovalStatus.PENDING);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("count", listings.size());
            response.put("properties", listings);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching pending property listings: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put(
                    "message",
                    "Failed to fetch pending property listings: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_PROPERTY_APPROVALS')")
    public ResponseEntity<?> getPropertyListing(@PathVariable Long id) {
        try {
            java.util.Optional<PropertyListingDto> listingDto =
                    propertyListingService.getPropertyListingById(id);

            if (listingDto.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Property listing not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
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

    @PostMapping("/{id}/approve")
    @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_PROPERTY_APPROVALS')")
    public ResponseEntity<?> approvePropertyListing(@PathVariable Long id) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User admin = (User) auth.getPrincipal();

            PropertyListing approvedListing =
                    propertyListingService.approvePropertyListing(id, admin);
            PropertyListingDto listingDto =
                    propertyListingService
                            .getPropertyListingById(approvedListing.getId())
                            .orElseThrow(
                                    () ->
                                            new RuntimeException(
                                                    "Failed to retrieve approved listing"));

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Property listing approved successfully");
            response.put("property", listingDto);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Error approving property listing: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());

            HttpStatus status =
                    e.getMessage().contains("not found")
                            ? HttpStatus.NOT_FOUND
                            : HttpStatus.INTERNAL_SERVER_ERROR;

            return ResponseEntity.status(status).body(errorResponse);
        } catch (Exception e) {
            log.error("Error approving property listing: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put(
                    "message", "Failed to approve property listing: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_PROPERTY_APPROVALS')")
    public ResponseEntity<?> rejectPropertyListing(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> requestBody) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User admin = (User) auth.getPrincipal();

            String reason = requestBody != null ? requestBody.get("reason") : null;
            if (reason == null || reason.trim().isEmpty()) {
                reason = "Property listing does not meet our requirements";
            }

            PropertyListing rejectedListing =
                    propertyListingService.rejectPropertyListing(id, reason, admin);
            PropertyListingDto listingDto =
                    propertyListingService
                            .getPropertyListingById(rejectedListing.getId())
                            .orElseThrow(
                                    () ->
                                            new RuntimeException(
                                                    "Failed to retrieve rejected listing"));

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Property listing rejected successfully");
            response.put("property", listingDto);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Error rejecting property listing: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());

            HttpStatus status =
                    e.getMessage().contains("not found")
                            ? HttpStatus.NOT_FOUND
                            : HttpStatus.INTERNAL_SERVER_ERROR;

            return ResponseEntity.status(status).body(errorResponse);
        } catch (Exception e) {
            log.error("Error rejecting property listing: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put(
                    "message", "Failed to reject property listing: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}

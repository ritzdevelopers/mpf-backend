package com.mypropertyfact.estate.controllers;

import com.mypropertyfact.estate.dtos.SuccessResponse;
import com.mypropertyfact.estate.entities.Enquery;
import com.mypropertyfact.estate.entities.User;
import com.mypropertyfact.estate.models.Response;
import com.mypropertyfact.estate.services.EnquiryAccessService;
import com.mypropertyfact.estate.services.EnquiryService;
import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/enquiry")
@RequiredArgsConstructor
public class EnquiryController {

    private final EnquiryService enquiryService;
    private final EnquiryAccessService enquiryAccessService;

    @Value("${crm.webhook.key:${MPF_CRM_WEBHOOK_KEY:}}")
    private String crmExportKey;

    @GetMapping("/get-all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Enquery>> getAll(HttpServletRequest request) {
        User user = requireUser();
        assertEnquiryAccess(user, request);
        return new ResponseEntity<>(enquiryService.getAll(), HttpStatus.OK);
    }

    @GetMapping("/crm-export")
    public ResponseEntity<List<Enquery>> crmExport(HttpServletRequest request) {
        String key = request.getHeader("x-mpf-crm-key");
        if (!StringUtils.hasText(crmExportKey) || !crmExportKey.equals(key)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return new ResponseEntity<>(enquiryService.getAll(), HttpStatus.OK);
    }

    @PostMapping("/post")
    public ResponseEntity<Response> addUpdate(@RequestBody Enquery enquery) {
        // Current website forms use this endpoint; default source is WEBSITE.
        return new ResponseEntity<>(enquiryService.addUpdate(enquery, "WEBSITE"), HttpStatus.OK);
    }

    @PostMapping("/post-app")
    public ResponseEntity<Response> addUpdateFromApp(@RequestBody Enquery enquery) {
        // Dedicated endpoint for mobile app leads.
        return new ResponseEntity<>(enquiryService.addUpdate(enquery, "APP"), HttpStatus.OK);
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Response> deleteEnquiry(@PathVariable int id, HttpServletRequest request) {
        User user = requireUser();
        assertEnquiryAccess(user, request);
        return new ResponseEntity<>(enquiryService.deleteEnquiry(id), HttpStatus.OK);
    }

    @PutMapping("/update-status/{enquiryId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SuccessResponse> updateStatus(
            @PathVariable("enquiryId") int enquiryId,
            @RequestBody Map<String, String> requestBody,
            HttpServletRequest request) {
        User user = requireUser();
        assertEnquiryAccess(user, request);
        return ResponseEntity.ok(enquiryService.updateStatus(enquiryId, requestBody.get("status")));
    }

    @GetMapping("/by-property/{propertyId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Enquery>> getByPropertyId(
            @PathVariable Long propertyId, HttpServletRequest request) {
        User user = requireUser();
        assertEnquiryAccess(user, request);
        return new ResponseEntity<>(enquiryService.getByPropertyId(propertyId), HttpStatus.OK);
    }

    @GetMapping("/get-user-leads")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getUserLeads() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        if (enquiryService.getUserLeads(email) == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new Response(0, "Invalid token", 0));
        }
        return new ResponseEntity<>(enquiryService.getUserLeads(email), HttpStatus.OK);
    }

    private static User requireUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User)) {
            throw new org.springframework.security.authentication.AuthenticationCredentialsNotFoundException(
                    "Not authenticated");
        }
        return (User) auth.getPrincipal();
    }

    private void assertEnquiryAccess(User user, HttpServletRequest request) {
        if (!enquiryAccessService.canAccessEnquiries(user, request)) {
            throw new AccessDeniedException(
                    "Enquiries access denied. Enter your 4-digit code on the Enquiries page, or sign in as Super Admin.");
        }
    }
}

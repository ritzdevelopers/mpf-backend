package com.mypropertyfact.estate.services;

import com.mypropertyfact.estate.dtos.SuccessResponse;
import com.mypropertyfact.estate.entities.Enquery;
import com.mypropertyfact.estate.entities.PropertyListing;
import com.mypropertyfact.estate.entities.User;
import com.mypropertyfact.estate.models.Response;
import com.mypropertyfact.estate.repositories.EnqueryRepository;
import com.mypropertyfact.estate.repositories.PropertyListingRepository;
import com.mypropertyfact.estate.repositories.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnquiryService {

    private final EnqueryRepository enqueryRepository;

    private final SendEmailHandler sendEmailHandler; 

    private final UserRepository userRepository;

    private final PropertyListingRepository propertyListingRepository;
    private final LeadService leadService;
    private final CrmIntegrationService crmIntegrationService;
    private static final String SOURCE_WEBSITE = "WEBSITE";
    private static final String SOURCE_APP = "APP";
    private static final String SUCCESS_MESSAGE = "Enquiry sent successfully";

    public List<Enquery> getAll() {
        return enqueryRepository.findAll();
    }

    public List<Enquery> getByPropertyId(Long propertyId) {
        return enqueryRepository.findByPropertyId(propertyId);
    }

    public Response addUpdate(Enquery enquery) {
        return addUpdate(enquery, null);
    }

    public Response addUpdate(Enquery enquery, String forcedSource) {
        Response response = new Response();
        try {
            String source = normalizeLeadSource(forcedSource != null ? forcedSource : enquery.getEnquiryFrom());
            enquery.setEnquiryFrom(source);
            if (enquery.getId() > 0) {
                Enquery dbEnquery = enqueryRepository.findById(enquery.getId()).orElse(null);
                if (dbEnquery != null) {
                    dbEnquery.setName(enquery.getName());
                    dbEnquery.setEmail(enquery.getEmail());
                    dbEnquery.setPhone(enquery.getPhone());
                    dbEnquery.setMessage(enquery.getMessage());
                    dbEnquery.setPageName(enquery.getPageName());
                    dbEnquery.setUpdatedAt(LocalDateTime.now());
                    dbEnquery.setEnquiryFrom(source);
                    dbEnquery.setProjectLink(enquery.getProjectLink());
                    dbEnquery.setPropertyId(enquery.getPropertyId());
                    if (enquery.getMetadataJson() != null) {
                        dbEnquery.setMetadataJson(enquery.getMetadataJson());
                    }
                    if (enquery.getWhatsapp() != null) {
                        dbEnquery.setWhatsapp(enquery.getWhatsapp());
                    }
                    Enquery saved = enqueryRepository.save(dbEnquery);
                    notifyEnquiryIntegrationsAsync(saved, source);
                    response.setIsSuccess(1);
                    response.setMessage(SUCCESS_MESSAGE);
                } else {
                    response.setMessage("No data found !!");
                }
            } else {
                Enquery saved = enqueryRepository.save(enquery);
//                sendEmailHandler.sendEmail(saved.getEmail(), "Thank you for giving details",
//                        "Hi, Thank you out team will get back to you");
                // Telegram + CRM run in background so the form can return immediately after DB save.
                notifyEnquiryIntegrationsAsync(saved, source);
                response.setIsSuccess(1);
                response.setMessage(SUCCESS_MESSAGE);
            }
        } catch (Exception e) {
            response.setMessage(e.getMessage());
        }
        return response;
    }

    /**
     * Push Telegram / CRM after the HTTP response. Failures must not block enquiry submit.
     */
    private void notifyEnquiryIntegrationsAsync(Enquery saved, String source) {
        CompletableFuture.runAsync(() -> {
            try {
                leadService.createLead(
                        saved.getName(),
                        saved.getPhone(),
                        saved.getEmail(),
                        saved.getPageName(),
                        "lead",
                        source,
                        saved.getProjectLink()
                );
            } catch (Exception e) {
                log.warn("Telegram lead notify failed for enquiry {}: {}", saved.getId(), e.getMessage());
            }
            try {
                crmIntegrationService.pushEnquiry(saved);
            } catch (Exception e) {
                log.warn("CRM push failed for enquiry {}: {}", saved.getId(), e.getMessage());
            }
        });
    }

    private String normalizeLeadSource(String raw) {
        String value = raw == null ? "" : raw.trim().toUpperCase();
        if (SOURCE_APP.equals(value)) return SOURCE_APP;
        if ("WEB".equals(value) || SOURCE_WEBSITE.equals(value) || value.isEmpty()) {
            return SOURCE_WEBSITE;
        }
        return SOURCE_WEBSITE;
    }

    public Response deleteEnquiry(int id) {
        try {
            Enquery dbEnquery = enqueryRepository.findById(id).orElse(null);
            if (dbEnquery != null) {
                enqueryRepository.deleteById(id);
                return new Response(1, "Enquiry deleted successfully...", 0);
            } else {
                throw new Exception("data already deleted or not found !!");
            }
        } catch (Exception e) {
            return new Response(0, e.getMessage(), 0);
        }
    }

    public SuccessResponse updateStatus(int enquiryId, String status) {
        SuccessResponse successResponse = new SuccessResponse();
        Optional<Enquery> enquiryById = enqueryRepository.findById(enquiryId);
        enquiryById.ifPresent(enquery -> {
            enquery.setStatus(status);
            enqueryRepository.save(enquery);
            successResponse.setIsSuccess(1);
            successResponse.setMessage("Status updated successfully...");
        });
        if (successResponse.getIsSuccess() != 1) {
            successResponse.setIsSuccess(0);
            successResponse.setMessage("Something went wrong while updating status !");
        }
        return successResponse;
    }

    public List<Enquery> getUserLeads(String email) {
        Optional<User> user = userRepository.findByEmail(email);
        if(user.isPresent()){
            List<PropertyListing> propertyListings = propertyListingRepository.findByUserId(user.get().getId());
            return enqueryRepository.findByPropertyIdIn(propertyListings.stream().map(PropertyListing::getId).collect(Collectors.toList()));
        }
        return null;
    }
}
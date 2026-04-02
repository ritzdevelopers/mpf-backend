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
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EnquiryService {

    private final EnqueryRepository enqueryRepository;

    private final SendEmailHandler sendEmailHandler;

    private final UserRepository userRepository;

    private final PropertyListingRepository propertyListingRepository;
    private final LeadService leadService;

    public List<Enquery> getAll() {
        return enqueryRepository.findAll();
    }

    public List<Enquery> getByPropertyId(Long propertyId) {
        return enqueryRepository.findByPropertyId(propertyId);
    }

    public Response addUpdate(Enquery enquery) {
        Response response = new Response();
        try {
            if (enquery.getId() > 0) {
                Enquery dbEnquery = enqueryRepository.findById(enquery.getId()).orElse(null);
                if (dbEnquery != null) {
                    dbEnquery.setName(enquery.getName());
                    dbEnquery.setEmail(enquery.getEmail());
                    dbEnquery.setPhone(enquery.getPhone());
                    dbEnquery.setMessage(enquery.getMessage());
                    dbEnquery.setPageName(enquery.getPageName());
                    dbEnquery.setUpdatedAt(LocalDateTime.now());
                    dbEnquery.setEnquiryFrom(enquery.getEnquiryFrom());
                    dbEnquery.setProjectLink(enquery.getProjectLink());
                    dbEnquery.setPropertyId(enquery.getPropertyId());
                    enqueryRepository.save(dbEnquery);
                    response.setIsSuccess(1);
                    leadService.createLead(
                            enquery.getName(),
                            enquery.getPhone(),
                            enquery.getEmail(),
                            enquery.getPageName(),
                            "lead",
                            enquery.getEnquiryFrom(),
                            enquery.getProjectLink()
                    );
                    response.setMessage("Data updated successfully...");
                } else {
                    response.setMessage("No data found !!");
                }
            } else {
                enqueryRepository.save(enquery);
//                sendEmailHandler.sendEmail(enquery.getEmail(), "Thank you for giving details",
//                        "Hi, Thank you out team will get back to you");
                try {
                    leadService.createLead(
                            enquery.getName(),
                            enquery.getPhone(),
                            enquery.getEmail(),
                            enquery.getPageName(),
                            "lead",
                            enquery.getEnquiryFrom(),
                            enquery.getProjectLink()
                    );
                } catch (Exception ignored) {
                    // Keep existing enquiry flow unchanged if Telegram fails.
                }
                response.setIsSuccess(1);
                response.setMessage("Data saved successfully...");
            }
        } catch (Exception e) {
            response.setMessage(e.getMessage());
        }
        return response;
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
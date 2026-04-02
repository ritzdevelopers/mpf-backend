package com.mypropertyfact.estate.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class LeadService {

    @Autowired
    private TelegramService telegramService;

    public void createLead(String name, String phone, String email, String service) {
        createLead(name, phone, email, service, "lead", null, null, null);
    }

    public void createLead(String name, String phone, String email, String service, String sourceType) {
        createLead(name, phone, email, service, sourceType, null, null, null);
    }

    public void createLead(String name, String phone, String email, String service, String sourceType,
            String enquiryFrom, String projectLink) {
        createLead(name, phone, email, service, sourceType, enquiryFrom, projectLink, null);
    }

    public void createLead(String name, String phone, String email, String service, String sourceType,
            String resumeLink) {
        createLead(name, phone, email, service, sourceType, null, null, resumeLink);
    }

    /**
     * @param resumeLink optional full URL to the candidate resume (e.g. career applications)
     */
    public void createLead(String name, String phone, String email, String service, String sourceType,
            String enquiryFrom, String projectLink, String resumeLink) {

        // save lead
        // leadRepository.save(lead);

        String message = "🚀 NEW LEAD RECEIVED\n\n"
                + "Lead Details\n"
                + "--------------------\n"
                + "🏷️ Type: " + valueOrNA(sourceType) + "\n"
                + "👤 Name: " + valueOrNA(name) + "\n"
                + "📞 Phone: " + valueOrNA(phone) + "\n"
                + "📧 Email: " + valueOrNA(email) + "\n"
                + "🛎️ Service: " + valueOrNA(service) + "\n"
                + "🌐 Enquiry From: " + valueOrNA(enquiryFrom) + "\n"
                + "🔗 Project Link: " + valueOrNA(projectLink);

        if (StringUtils.hasText(resumeLink)) {
            message += "\n📎 Resume: " + resumeLink;
        }

        telegramService.sendLeadNotification(message);
    }

    private String valueOrNA(String value) {
        return StringUtils.hasText(value) ? value : "N/A";
    }
}
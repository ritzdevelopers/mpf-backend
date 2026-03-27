package com.mypropertyfact.estate.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class LeadService {

    @Autowired
    private TelegramService telegramService;

    public void createLead(String name, String phone, String email, String service) {
        createLead(name, phone, email, service, "lead", null);
    }

    public void createLead(String name, String phone, String email, String service, String sourceType) {
        createLead(name, phone, email, service, sourceType, null);
    }

    /**
     * @param resumeLink optional full URL to the candidate resume (e.g. career applications)
     */
    public void createLead(String name, String phone, String email, String service, String sourceType,
            String resumeLink) {

        // save lead
        // leadRepository.save(lead);

        String message = "🚀 NEW LEAD\n\n"
                + "🏷️ Type: " + sourceType + "\n"
                + "📌 Source: " + sourceType + "\n"
                + "👤 Name: " + name + "\n"
                + "📞 Phone: " + phone + "\n"
                + "📧 Email: " + email + "\n"
                + "📍 Service: " + service;

        if (StringUtils.hasText(resumeLink)) {
            message += "\n📎 Resume: " + resumeLink;
        }

        telegramService.sendLeadNotification(message);
    }
}
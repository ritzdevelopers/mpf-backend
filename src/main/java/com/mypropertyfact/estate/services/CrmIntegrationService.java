package com.mypropertyfact.estate.services;

import com.mypropertyfact.estate.entities.Enquery;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class CrmIntegrationService {

    @Value("${crm.webhook.url:${CRM_WEBHOOK_URL:}}")
    private String webhookUrl;

    @Value("${crm.webhook.key:${MPF_CRM_WEBHOOK_KEY:}}")
    private String webhookKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public void pushEnquiry(Enquery enquiry) {
        if (!StringUtils.hasText(webhookUrl) || !StringUtils.hasText(webhookKey) || enquiry == null) {
            return;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-mpf-crm-key", webhookKey);

            Map<String, Object> body = new HashMap<>();
            body.put("externalId", enquiry.getId());
            body.put("name", enquiry.getName());
            body.put("email", enquiry.getEmail());
            body.put("phone", enquiry.getPhone());
            body.put("message", enquiry.getMessage());
            body.put("enquiryFrom", enquiry.getEnquiryFrom());
            body.put("pageName", enquiry.getPageName());
            body.put("projectLink", enquiry.getProjectLink());
            body.put("status", enquiry.getStatus());
            if (enquiry.getCreatedAt() != null) {
                body.put("createdAt", enquiry.getCreatedAt().toString());
            }

            restTemplate.postForObject(webhookUrl, new HttpEntity<>(body, headers), String.class);
        } catch (Exception ignored) {
            // Keep enquiry flow unchanged if CRM push fails.
        }
    }
}

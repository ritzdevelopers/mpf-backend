package com.mypropertyfact.estate.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import jakarta.mail.internet.MimeMessage;

import java.util.concurrent.CompletableFuture;

@Component
@Slf4j
@RequiredArgsConstructor
public class SendEmailHandler {

    private final JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.mail.from-name:My Property Fact}")
    private String fromName;

    /**
     * Sends HTML email. Returns {@code true} only when SMTP accepts the message.
     */
    public boolean sendEmail(String to, String subject, String body) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true);
            javaMailSender.send(message);
            log.info("Email sent successfully to {}", to);
            return true;
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage(), e);
            return false;
        }
    }

    /** Fire-and-forget for non-critical notifications (e.g. enquiry acknowledgements). */
    public void sendEmailAsync(String to, String subject, String body) {
        CompletableFuture.runAsync(() -> sendEmail(to, subject, body));
    }
}

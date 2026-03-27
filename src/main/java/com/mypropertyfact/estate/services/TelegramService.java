package com.mypropertyfact.estate.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Service
public class TelegramService {

    @Value("${telegram.bot.token:${TELEGRAM_BOT_TOKEN:}}")
    private String botToken;

    @Value("${telegram.chat.id:${TELEGRAM_CHAT_ID:}}")
    private String chatId;

    private final RestTemplate restTemplate = new RestTemplate();

    public void sendLeadNotification(String message) {
        if (!StringUtils.hasText(botToken) || !StringUtils.hasText(chatId)) {
            return;
        }

        String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";

        Map<String, String> body = new HashMap<>();
        body.put("chat_id", chatId);
        body.put("text", message);

        restTemplate.postForObject(url, body, String.class);
    }
}
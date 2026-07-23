package com.stjohn.qalioub.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class WhatsAppService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppService.class);

    @Value("${app.whatsapp.access-token}")
    private String accessToken;

    @Value("${app.whatsapp.phone-number-id}")
    private String phoneNumberId;

    @Value("${app.whatsapp.api-url}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public void sendOtp(String toPhone, String otp) {
        String url = apiUrl + "/" + phoneNumberId + "/messages";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        Map<String, Object> body = Map.of(
            "messaging_product", "whatsapp",
            "to", toPhone,
            "type", "text",
            "text", Map.of("body", "Your OTP code is: *" + otp + "*\nValid for 5 minutes.")
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            log.info("WhatsApp message sent to {}. Status: {}", toPhone, response.getStatusCode());
        } catch (Exception e) {
            log.error("Failed to send WhatsApp message to {}: {}", toPhone, e.getMessage());
        }
    }
}

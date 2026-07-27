package com.stjohn.qalioub.service;

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.stjohn.qalioub.model.WhySmsRequestBody;

@Service
public class SmsService {

    private static final Logger log = LoggerFactory.getLogger(SmsService.class);

    @Value("${app.sms.api-url}")
    private String apiUrl;

    @Value("${app.sms.bearer-token}")
    private String bearerToken;

    @Value("${app.sms.sender-id}")
    private String senderId;

    private final RestClient restClient = RestClient.builder()
            .requestInterceptor((request, body, execution) -> {
                log.info(">>> {} {}\nHeaders: {}\nBody: {}",
                        request.getMethod(), request.getURI(),
                        request.getHeaders(),
                        new String(body, StandardCharsets.UTF_8));
                return execution.execute(request, body);
            })
            .build();

    public void sendOtp(String toPhone, String otp) {
        toPhone = "+2" + toPhone;
        String message= "مسرحية الصارخ \n رمز التحقق: " + otp + "\n صالح لمدة 5 دقائق";

        WhySmsRequestBody body = new WhySmsRequestBody();
        body.setRecipient(toPhone);
        body.setSenderId(senderId);
        body.setType("plain");
        body.setMessage(message);

        try {
            String response = restClient.post()
                    .uri(apiUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + bearerToken)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            log.info("\n SMS sent to {}. Body: {}", toPhone, response);
        } catch (Exception e) {
            log.error("Failed to send SMS to {}: {}", toPhone, e.getMessage());
        }
    }
}

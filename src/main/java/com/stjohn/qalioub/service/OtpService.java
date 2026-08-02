package com.stjohn.qalioub.service;

import com.stjohn.qalioub.entity.OtpRecord;
import com.stjohn.qalioub.repository.OtpRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);

    private final OtpRepository otpRepository;
    private final SmsService smsService;

    @Value("${app.otp.expiration-minutes}")
    private int expirationMinutes;

    @Value("${app.otp.length}")
    private int otpLength;

    private final SecureRandom random = new SecureRandom();

    public OtpService(OtpRepository otpRepository, SmsService smsService) {
        this.otpRepository = otpRepository;
        this.smsService = smsService;
    }

    public void generateAndSend(String phone) {
        otpRepository.invalidateAllForPhone(phone);

        String otp = generateOtp();

        OtpRecord record = new OtpRecord();
        record.setPhone(phone);
        record.setOtp(otp);
        record.setExpiresAt(LocalDateTime.now().plusMinutes(expirationMinutes));
        record.setUsed(false);
        otpRepository.save(record);

        smsService.sendOtp(phone, otp);
    }

    public boolean verify(String phone, String otp) {
        return otpRepository.findTopByPhoneAndUsedFalseOrderByExpiresAtDesc(phone)
                .filter(record -> !record.isUsed())
                .filter(record -> record.getExpiresAt().isAfter(LocalDateTime.now()))
                .filter(record -> record.getOtp().equals(otp))
                .map(record -> {
                    record.setUsed(true);
                    otpRepository.save(record);
                    return true;
                })
                .orElse(false);
    }

    private String generateOtp() {
        int max = (int) Math.pow(10, otpLength);
        int min = (int) Math.pow(10, otpLength - 1);
        return String.valueOf(min + random.nextInt(max - min));
    }
}

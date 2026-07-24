package com.stjohn.qalioub.service;

import com.stjohn.qalioub.entity.User;
import com.stjohn.qalioub.repository.UserRepository;
import com.stjohn.qalioub.security.JwtUtil;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final OtpService otpService;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository, OtpService otpService, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.otpService = otpService;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Creates or updates the user record, then triggers OTP generation and sending.
     */
    public String requestOtp(String phone) {
        // Find existing user or create new one
        User user = userRepository.findByPhone(phone).orElseGet(User::new);
        user.setPhone(phone);
        userRepository.save(user);

        // Generate OTP and send to WhatsApp
        return otpService.generateAndSend(phone);
    }

    /**
     * Verifies OTP and returns the authenticated User if valid, null otherwise.
     */
    public User verifyOtp(String phone, String otp) {
        if (!otpService.verify(phone, otp)) {
            return null;
        }

        return userRepository.findByPhone(phone)
                .orElseThrow(() -> new IllegalStateException("User not found after OTP verification"));
    }

    public String generateToken(User user) {
        return jwtUtil.generateToken(user.getPhone(), user.getId(), user.getName());
    }

    /**
     * Sets the user's name and marks first login as complete.
     */
    public User updateName(String phone, String name) {
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new IllegalStateException("User not found: " + phone));
        user.setName(name);
        user.setFirstLogin(false);
        return userRepository.save(user);
    }
}

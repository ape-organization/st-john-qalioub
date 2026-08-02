package com.stjohn.qalioub.controller;

import com.stjohn.qalioub.api.AuthApi;
import com.stjohn.qalioub.api.model.AuthResponse;
import com.stjohn.qalioub.api.model.MessageResponse;
import com.stjohn.qalioub.api.model.OtpRequest;
import com.stjohn.qalioub.api.model.OtpVerifyRequest;
import com.stjohn.qalioub.api.model.UserDto;
import com.stjohn.qalioub.entity.User;
import com.stjohn.qalioub.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class AuthController implements AuthApi {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public ResponseEntity<MessageResponse> requestOtp(OtpRequest otpRequest) {
        authService.requestOtp(otpRequest.getPhone());

        MessageResponse response = new MessageResponse();
        response.setMessage("OTP sent to WhatsApp number " + otpRequest.getPhone());
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<AuthResponse> verifyOtp(OtpVerifyRequest otpVerifyRequest) {
        User user = authService.verifyOtp(otpVerifyRequest.getPhone(), otpVerifyRequest.getOtp());

        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        String token = authService.generateToken(user);

        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setFirstLogin(user.isFirstLogin());
        response.setUser(toDto(user));

        return ResponseEntity.ok(response);
    }

    static UserDto toDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setPhone(user.getPhone());
        dto.setFirstLogin(user.isFirstLogin());
        dto.setRole(UserDto.RoleEnum.valueOf(user.getRole()));
        dto.setBalance(user.getBalance());
        return dto;
    }
}

package com.stjohn.qalioub.controller;

import com.stjohn.qalioub.api.UserApi;
import com.stjohn.qalioub.api.model.UpdateNameRequest;
import com.stjohn.qalioub.api.model.UserDto;
import com.stjohn.qalioub.entity.User;
import com.stjohn.qalioub.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class UserController implements UserApi {

    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public ResponseEntity<UserDto> updateName(UpdateNameRequest updateNameRequest) {
        // The authenticated user is injected by Spring Security via JwtAuthFilter
        User principal = getAuthenticatedUser();
        User updated = authService.updateName(principal.getPhone(), updateNameRequest.getName());
        return ResponseEntity.ok(AuthController.toDto(updated));
    }

    private User getAuthenticatedUser() {
        org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder
                        .getContext().getAuthentication();
        return (User) auth.getPrincipal();
    }
}

package com.stjohn.qalioub.controller;

import com.stjohn.qalioub.api.UserApi;
import com.stjohn.qalioub.api.model.ReservationDto;
import com.stjohn.qalioub.api.model.UpdateNameRequest;
import com.stjohn.qalioub.api.model.UserDto;
import com.stjohn.qalioub.entity.User;
import com.stjohn.qalioub.service.AuthService;
import com.stjohn.qalioub.service.ReservationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class UserController implements UserApi {

    private final AuthService authService;
    private final ReservationService reservationService;

    public UserController(AuthService authService, ReservationService reservationService) {
        this.authService = authService;
        this.reservationService = reservationService;
    }

    @Override
    public ResponseEntity<UserDto> updateName(UpdateNameRequest updateNameRequest) {
        // The authenticated user is injected by Spring Security via JwtAuthFilter
        User principal = getAuthenticatedUser();
        User updated = authService.updateName(principal.getPhone(), updateNameRequest.getName());
        return ResponseEntity.ok(AuthController.toDto(updated));
    }

    @Override
    public ResponseEntity<List<ReservationDto>> getUserReservations() {
        User principal = getAuthenticatedUser();
        List<ReservationDto> reservations = reservationService.getUserReservations(principal).stream()
                .map(SeatController::toReservationDto)
                .toList();
        return ResponseEntity.ok(reservations);
    }

    private User getAuthenticatedUser() {
        org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder
                        .getContext().getAuthentication();
        return (User) auth.getPrincipal();
    }
}

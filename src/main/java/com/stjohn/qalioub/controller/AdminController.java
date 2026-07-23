package com.stjohn.qalioub.controller;

import com.stjohn.qalioub.api.AdminApi;
import com.stjohn.qalioub.api.model.ReservationDto;
import com.stjohn.qalioub.entity.Reservation;
import com.stjohn.qalioub.service.ReservationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class AdminController implements AdminApi {

    private final ReservationService reservationService;

    public AdminController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @Override
    public ResponseEntity<List<ReservationDto>> getAllReservations() {
        List<ReservationDto> reservations = reservationService.getAllReservations().stream()
                .map(SeatController::toReservationDto)
                .toList();
        return ResponseEntity.ok(reservations);
    }

    @Override
    public ResponseEntity<ReservationDto> confirmReservation(Long id) {
        try {
            Reservation reservation = reservationService.confirmReservation(id);
            return ResponseEntity.ok(SeatController.toReservationDto(reservation));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}

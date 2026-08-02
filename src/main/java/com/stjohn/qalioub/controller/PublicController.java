package com.stjohn.qalioub.controller;

import com.stjohn.qalioub.api.PublicApi;
import com.stjohn.qalioub.api.model.ReservationDto;
import com.stjohn.qalioub.entity.Reservation;
import com.stjohn.qalioub.service.ReservationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class PublicController implements PublicApi {

    private final ReservationService reservationService;

    public PublicController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @Override
    public ResponseEntity<ReservationDto> getTicketByToken(String token) {
        try {
            Reservation reservation = reservationService.getReservationByTicketToken(token);
            if (reservation.getStatus() != Reservation.Status.CONFIRMED) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(SeatController.toReservationDto(reservation));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}

package com.stjohn.qalioub.controller;

import com.stjohn.qalioub.api.PublicApi;
import com.stjohn.qalioub.api.model.ReservationDto;
import com.stjohn.qalioub.entity.Reservation;
import com.stjohn.qalioub.service.ReservationService;
import com.stjohn.qalioub.service.SeatExportCache;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class PublicController implements PublicApi {

    private final ReservationService reservationService;
    private final SeatExportCache seatExportCache;

    public PublicController(ReservationService reservationService, SeatExportCache seatExportCache) {
        this.reservationService = reservationService;
        this.seatExportCache = seatExportCache;
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

    @GetMapping(value = "/public/seats-export/{token}", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> downloadSeatsExport(@PathVariable String token) {
        byte[] bytes = seatExportCache.getAndRemove(token);
        if (bytes == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"seats.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }
}

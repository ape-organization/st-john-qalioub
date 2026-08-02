package com.stjohn.qalioub.controller;

import com.stjohn.qalioub.api.SeatApi;
import com.stjohn.qalioub.api.model.ReservationDto;
import com.stjohn.qalioub.api.model.ReserveSeatsRequest;
import com.stjohn.qalioub.api.model.SeatDto;
import com.stjohn.qalioub.entity.Reservation;
import com.stjohn.qalioub.entity.Seat;
import com.stjohn.qalioub.entity.User;
import com.stjohn.qalioub.service.ReservationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZoneOffset;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class SeatController implements SeatApi {

    private final ReservationService reservationService;

    public SeatController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @Override
    public ResponseEntity<ReservationDto> reserveSeats(ReserveSeatsRequest reserveSeatsRequest) {
        User user = getAuthenticatedUser();
        try {
            Reservation reservation = reservationService.reserveSeats(user, reserveSeatsRequest.getSeatLabels(), reserveSeatsRequest.getNotes());
            return ResponseEntity.ok(toReservationDto(reservation));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Override
    public ResponseEntity<List<SeatDto>> getReservedSeats() {
        List<SeatDto> seats = reservationService.getAllSeatsWithStatus().stream()
                .map(entry -> toSeatDto(entry.seat(), entry.reservationStatus()))
                .toList();
        return ResponseEntity.ok(seats);
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (User) authentication.getPrincipal();
    }

    static SeatDto toSeatDto(Seat seat, Reservation.Status reservationStatus) {
        SeatDto dto = new SeatDto();
        dto.setId(seat.getId());
        dto.setLabel(seat.getLabel());
        dto.setStatus(reservationStatus == null
                ? SeatDto.StatusEnum.AVAILABLE
                : SeatDto.StatusEnum.valueOf(reservationStatus.name()));
        return dto;
    }

    static ReservationDto toReservationDto(Reservation reservation) {
        ReservationDto dto = new ReservationDto();
        dto.setId(reservation.getId());
        dto.setStatus(ReservationDto.StatusEnum.valueOf(reservation.getStatus().name()));
        dto.setCreatedAt(reservation.getCreatedAt().atOffset(ZoneOffset.UTC));
        dto.setExpiresAt(reservation.getExpiresAt().atOffset(ZoneOffset.UTC));
        dto.setSeats(reservation.getSeats().stream()
                .map(s -> toSeatDto(s, reservation.getStatus()))
                .toList());
        dto.setUser(AuthController.toDto(reservation.getUser()));
        dto.setTotalAmount(reservation.getTotalAmount());
        dto.setNotes(reservation.getNotes());
        dto.setPaymentLink(reservation.getPaymentLink());
        dto.setConfirmedBy(reservation.getConfirmedBy());
        dto.setAssignedTo(reservation.getAssignedTo());
        return dto;
    }
}

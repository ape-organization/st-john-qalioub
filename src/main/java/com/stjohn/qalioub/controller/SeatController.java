package com.stjohn.qalioub.controller;

import com.stjohn.qalioub.api.model.CarParkingDto;
import com.stjohn.qalioub.api.model.ReservationDto;
import com.stjohn.qalioub.api.model.SeatDto;
import com.stjohn.qalioub.entity.CarParking;
import com.stjohn.qalioub.entity.Reservation;
import com.stjohn.qalioub.entity.Seat;
import com.stjohn.qalioub.entity.User;
import com.stjohn.qalioub.service.ReservationService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.ZoneOffset;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class SeatController {   // no longer implements SeatApi — avoids @RequestPart on plain text fields

    private final ReservationService reservationService;

    public SeatController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping(value = "/seats/reserve", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReservationDto> reserveSeats(
            @RequestParam("seatLabels") List<String> seatLabels,
            @RequestParam(value = "notes", required = false) String notes,
            @RequestPart(value = "drivingLicensePhotos", required = false)
                    List<MultipartFile> drivingLicensePhotos) {
        User user = getAuthenticatedUser();
        try {
            Reservation reservation = reservationService.reserveSeats(
                    user, seatLabels, notes, drivingLicensePhotos);
            return ResponseEntity.ok(toReservationDto(reservation));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping(value = "/seats/reserved", produces = MediaType.APPLICATION_JSON_VALUE)
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

    static CarParkingDto toCarParkingDto(CarParking cp) {
        CarParkingDto dto = new CarParkingDto();
        dto.setId(cp.getId());
        dto.setStatus(CarParkingDto.StatusEnum.valueOf(cp.getStatus().name()));
        dto.setConfirmedBy(cp.getConfirmedBy());
        dto.setDrivingLicensePhotoPath(cp.getDrivingLicensePhotoPath());
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
        dto.setTicketToken(reservation.getTicketToken());
        dto.setConsumedSeats(reservation.getConsumedSeats().stream()
                .map(s -> toSeatDto(s, Reservation.Status.CONFIRMED))
                .toList());
        dto.setCarParkings(reservation.getCarParkings().stream()
                .map(SeatController::toCarParkingDto)
                .toList());
        return dto;
    }
}


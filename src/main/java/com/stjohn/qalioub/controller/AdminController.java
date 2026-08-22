package com.stjohn.qalioub.controller;

import com.stjohn.qalioub.api.AdminApi;
import com.stjohn.qalioub.api.model.BalanceResponse;
import com.stjohn.qalioub.api.model.ConsumeSeatsRequest;
import com.stjohn.qalioub.api.model.CreateTransferRequest;
import com.stjohn.qalioub.api.model.ReservationDto;
import com.stjohn.qalioub.api.model.SmsBroadcastResponse;
import com.stjohn.qalioub.api.model.TransferDto;
import com.stjohn.qalioub.entity.Reservation;
import com.stjohn.qalioub.entity.User;
import com.stjohn.qalioub.service.PdfService;
import com.stjohn.qalioub.service.ReservationService;
import com.stjohn.qalioub.service.SmsService;
import com.stjohn.qalioub.service.TransferService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class AdminController implements AdminApi {

    private final ReservationService reservationService;
    private final TransferService transferService;
    private final SmsService smsService;
    private final PdfService pdfService;

    public AdminController(ReservationService reservationService, TransferService transferService,  SmsService smsService, PdfService pdfService) {
        this.reservationService = reservationService;
        this.transferService = transferService;
        this.smsService = smsService;
        this.pdfService = pdfService;
    }

    @Override
    public ResponseEntity<org.springframework.core.io.Resource> generateCarParkingsPdf() {
        byte[] pdf = pdfService.generateCarParkingsPdf();
        ByteArrayResource resource = new ByteArrayResource(pdf);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("car-parkings.pdf").build().toString())
                .body(resource);
    }

    @Override
    public ResponseEntity<List<ReservationDto>> getAllReservations() {
        List<ReservationDto> reservations = reservationService.getAllReservations().stream()
                .map(SeatController::toReservationDto)
                .toList();
        return ResponseEntity.ok(reservations);
    }

    @Override
    public ResponseEntity<ReservationDto> consumeSeats(Long id, ConsumeSeatsRequest consumeSeatsRequest) {
        try {
            Reservation reservation = reservationService.consumeSeats(id, consumeSeatsRequest.getSeatLabels());
            return ResponseEntity.ok(SeatController.toReservationDto(reservation));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Override
    public ResponseEntity<ReservationDto> confirmCarParkings(Long id) {
        User admin = getAuthenticatedUser();
        try {
            Reservation reservation = reservationService.confirmCarParkings(id, admin.getId());
            return ResponseEntity.ok(SeatController.toReservationDto(reservation));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Override
    public ResponseEntity<ReservationDto> confirmReservation(Long id) {
        User admin = getAuthenticatedUser();
        try {
            Reservation reservation = reservationService.confirmReservation(id, admin.getId());
            return ResponseEntity.ok(SeatController.toReservationDto(reservation));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Override
    public ResponseEntity<TransferDto> createTransfer(CreateTransferRequest createTransferRequest) {
        User admin = getAuthenticatedUser();
        try {
            var transfer = transferService.createTransfer(admin.getId(), createTransferRequest.getAmount());
            return ResponseEntity.ok(SuperAdminController.toTransferDto(transfer));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Override
    public ResponseEntity<BalanceResponse> getAdminBalance() {
        User admin = getAuthenticatedUser();
        BalanceResponse response = new BalanceResponse();
        response.setBalance(admin.getBalance());
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<SmsBroadcastResponse> broadcastSmsToConfirmed() {
        String message = "مسرحية الصارخ\nلحجز ركنة يوم العرض\nبرجاء ملئ هذه الاستمارة وقراءة التعليمات جيدا\nhttps://forms.gle/ExQ8WqjhTubtoKNa9";

        List<String> phones = reservationService.getConfirmedReservationPhones();
        phones.forEach(phone -> smsService.sendMessage(phone, message));

        SmsBroadcastResponse response = new SmsBroadcastResponse();
        response.setMessagesSent(phones.size());
        return ResponseEntity.ok(response);
    }

    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (User) auth.getPrincipal();
    }
}

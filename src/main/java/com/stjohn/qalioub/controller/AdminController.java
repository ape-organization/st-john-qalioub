package com.stjohn.qalioub.controller;

import com.stjohn.qalioub.api.AdminApi;
import com.stjohn.qalioub.api.model.BalanceResponse;
import com.stjohn.qalioub.api.model.ConsumeSeatsRequest;
import com.stjohn.qalioub.api.model.CreateTransferRequest;
import com.stjohn.qalioub.api.model.ReservationDto;
import com.stjohn.qalioub.api.model.SmsBroadcastResponse;
import com.stjohn.qalioub.api.model.TransferDto;
import com.stjohn.qalioub.entity.Reservation;
import com.stjohn.qalioub.entity.Seat;
import com.stjohn.qalioub.entity.User;
import com.stjohn.qalioub.repository.SeatRepository;
import com.stjohn.qalioub.service.ReservationService;
import com.stjohn.qalioub.service.SmsService;
import com.stjohn.qalioub.service.TransferService;
import com.stjohn.qalioub.service.SeatExportCache;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class AdminController implements AdminApi {

    private final ReservationService reservationService;
    private final TransferService transferService;
    private final SmsService smsService;
    private final SeatRepository seatRepository;

    private final SeatExportCache seatExportCache;

    public AdminController(ReservationService reservationService,
                           TransferService transferService,
                           SmsService smsService,
                           SeatRepository seatRepository,
                           SeatExportCache seatExportCache) {
        this.reservationService = reservationService;
        this.transferService = transferService;
        this.smsService = smsService;
        this.seatRepository = seatRepository;
        this.seatExportCache = seatExportCache;
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

    @GetMapping("/admin/seats/export")
    public ResponseEntity<Map<String, String>> exportSeats(HttpServletRequest request) {
        List<Seat> seats = seatRepository.findAll(Sort.by("label"));

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Seats");

            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("ticket");
            header.createCell(1).setCellValue("seat");
            header.createCell(2).setCellValue("place");

            int rowIdx = 1;
            for (Seat seat : seats) {
                String label = seat.getLabel();
                String section;
                String number;
                if (label.startsWith("STAGE-")) {
                    section = "مسرح";
                    number = label.substring("STAGE-".length());
                } else if (label.startsWith("BAL-")) {
                    section = "بلكونة";
                    number = label.substring("BAL-".length());
                } else {
                    int dash = label.indexOf('-');
                    section = dash > 0 ? label.substring(0, dash) : label;
                    number = dash > 0 ? label.substring(dash + 1) : label;
                }

                Row row = sheet.createRow(rowIdx);
                row.createCell(0).setCellValue(String.format("%04d", rowIdx));
                row.createCell(1).setCellValue(number);
                row.createCell(2).setCellValue(section);
                rowIdx++;
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);

            String token = seatExportCache.store(out.toByteArray());
            String base = request.getScheme() + "://" + request.getServerName()
                    + (request.getServerPort() != 80 && request.getServerPort() != 443
                       ? ":" + request.getServerPort() : "");
            String downloadUrl = base + "/api/v1/public/seats-export/" + token;

            return ResponseEntity.ok(Map.of("downloadUrl", downloadUrl));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (User) auth.getPrincipal();
    }
}

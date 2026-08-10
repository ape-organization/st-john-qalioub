package com.stjohn.qalioub.controller;

import com.stjohn.qalioub.api.SuperAdminApi;
import com.stjohn.qalioub.api.model.MessageResponse;
import com.stjohn.qalioub.api.model.TransferDto;
import com.stjohn.qalioub.entity.Transfer;
import com.stjohn.qalioub.entity.User;
import com.stjohn.qalioub.service.ReservationService;
import com.stjohn.qalioub.service.TransferService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZoneOffset;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class SuperAdminController implements SuperAdminApi {

    private final TransferService transferService;
    private final ReservationService reservationService;

    public SuperAdminController(TransferService transferService, ReservationService reservationService) {
        this.transferService = transferService;
        this.reservationService = reservationService;
    }

    @Override
    public ResponseEntity<List<TransferDto>> getAllTransfers() {
        List<TransferDto> transfers = transferService.getAllTransfers().stream()
                .map(SuperAdminController::toTransferDto)
                .toList();
        return ResponseEntity.ok(transfers);
    }

    @Override
    public ResponseEntity<TransferDto> confirmTransfer(Long id) {
        User superAdmin = getAuthenticatedUser();
        try {
            Transfer transfer = transferService.confirmTransfer(id, superAdmin.getId());
            return ResponseEntity.ok(toTransferDto(transfer));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Override
    public ResponseEntity<MessageResponse> deleteReservation(Long id) {
        try {
            reservationService.deleteReservation(id);
            MessageResponse response = new MessageResponse();
            response.setMessage("Reservation deleted successfully");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (User) auth.getPrincipal();
    }

    static TransferDto toTransferDto(Transfer transfer) {
        TransferDto dto = new TransferDto();
        dto.setId(transfer.getId());
        dto.setAdmin(AuthController.toDto(transfer.getAdmin()));
        dto.setAmount(transfer.getAmount());
        dto.setStatus(TransferDto.StatusEnum.valueOf(transfer.getStatus().name()));
        dto.setCreatedAt(transfer.getCreatedAt().atOffset(ZoneOffset.UTC));
        return dto;
    }
}

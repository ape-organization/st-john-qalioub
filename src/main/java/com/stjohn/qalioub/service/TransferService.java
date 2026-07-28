package com.stjohn.qalioub.service;

import com.stjohn.qalioub.entity.Transfer;
import com.stjohn.qalioub.entity.User;
import com.stjohn.qalioub.repository.TransferRepository;
import com.stjohn.qalioub.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransferService {

    private final TransferRepository transferRepository;
    private final UserRepository userRepository;

    public TransferService(TransferRepository transferRepository, UserRepository userRepository) {
        this.transferRepository = transferRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Transfer createTransfer(Long adminId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive");
        }

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Admin not found"));

        if (admin.getBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient balance");
        }

        Transfer transfer = new Transfer();
        transfer.setAdmin(admin);
        transfer.setAmount(amount);
        transfer.setStatus(Transfer.Status.PENDING);
        transfer.setCreatedAt(LocalDateTime.now());

        return transferRepository.save(transfer);
    }

    @Transactional(readOnly = true)
    public List<Transfer> getAllTransfers() {
        return transferRepository.findAll();
    }

    @Transactional
    public Transfer confirmTransfer(Long transferId, Long superAdminId) {
        Transfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new IllegalArgumentException("Transfer not found: " + transferId));

        if (transfer.getStatus() != Transfer.Status.PENDING) {
            throw new IllegalStateException("Only PENDING transfers can be confirmed");
        }

        User admin = userRepository.findById(transfer.getAdmin().getId())
                .orElseThrow(() -> new IllegalArgumentException("Admin not found"));
        User superAdmin = userRepository.findById(superAdminId)
                .orElseThrow(() -> new IllegalArgumentException("Super admin not found"));

        if (admin.getBalance().compareTo(transfer.getAmount()) < 0) {
            throw new IllegalStateException("Admin has insufficient balance for this transfer");
        }

        admin.setBalance(admin.getBalance().subtract(transfer.getAmount()));
        superAdmin.setBalance(superAdmin.getBalance().add(transfer.getAmount()));

        userRepository.save(admin);
        userRepository.save(superAdmin);

        transfer.setStatus(Transfer.Status.CONFIRMED);
        return transferRepository.save(transfer);
    }
}

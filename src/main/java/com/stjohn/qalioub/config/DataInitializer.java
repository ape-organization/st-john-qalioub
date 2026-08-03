package com.stjohn.qalioub.config;

import com.stjohn.qalioub.entity.Seat;
import com.stjohn.qalioub.entity.User;
import com.stjohn.qalioub.repository.SeatRepository;
import com.stjohn.qalioub.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DataInitializer implements ApplicationRunner {


    private final SeatRepository seatRepository;
    private final UserRepository userRepository;

    public DataInitializer(SeatRepository seatRepository, UserRepository userRepository) {
        this.seatRepository = seatRepository;
        this.userRepository = userRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (seatRepository.count() == 0) {
            seedStageSeats();
            seedBalconySeats();
        }
        seedAdminUsers();
    }

    // ── Stage ────────────────────────────────────────────────────────────────
    // Each row: center block (seats 1..centerEnd), left wing (odd), right wing (even).
    // Seat labels: "STAGE-B1", "STAGE-B11", "STAGE-B12", etc.
    private void seedStageSeats() {
        //              row   centerEnd  leftOddStart  leftOddEnd  rightEvenStart  rightEvenEnd
        seedStageRow("A", 0, 11, 21, 10, 20);
        seedStageRow("B", 10, 11, 23, 12, 24);
        seedStageRow("C", 9, 11, 23, 10, 22);
        seedStageRow("D", 10, 11, 25, 12, 26);
        seedStageRow("E", 9, 11, 25, 10, 24);
        seedStageRow("F", 10, 11, 27, 12, 28);
        seedStageRow("G", 9, 11, 27, 10, 26);
        seedStageRow("H", 10, 11, 27, 12, 28);
        seedStageRow("I", 9, 11, 27, 10, 26);
        seedStageRow("J", 9, 11, 29, 10, 28);
        seedStageRow("K", 9, 11, 29, 10, 28);
        seedStageRow("L", 9, 11, 29, 10, 28);
        seedStageRow("M", 9, 11, 29, 10, 28);
        seedStageRow("N", 9, 11, 31, 10, 30);
        seedStageRow("O", 9, 11, 31, 10, 30);
        seedStageRow("P", 9, 11, 31, 10, 30);
        seedStageRow("Q", 0, 11, 31, 10, 30);
        seedStageRow("R", 0, 1, 23, 2, 24);
    }

    /**
     * @param centerEnd      seats 1..centerEnd (all numbers). 0 = no center block.
     * @param leftOddStart   first odd seat number in the left wing
     * @param leftOddEnd     last odd seat number in the left wing
     * @param rightEvenStart first even seat number in the right wing
     * @param rightEvenEnd   last even seat number in the right wing
     */
    private void seedStageRow(String row, int centerEnd,
                              int leftOddStart, int leftOddEnd,
                              int rightEvenStart, int rightEvenEnd) {
        List<Seat> seats = new ArrayList<>();
        for (int i = 1; i <= centerEnd; i++) {
            seats.add(new Seat("STAGE-" + row + i));
        }
        for (int i = leftOddStart; i <= leftOddEnd; i += 2) {
            seats.add(new Seat("STAGE-" + row + i));
        }
        for (int i = rightEvenStart; i <= rightEvenEnd; i += 2) {
            seats.add(new Seat("STAGE-" + row + i));
        }
        seatRepository.saveAll(seats);
    }

    // ── Balcony ──────────────────────────────────────────────────────────────
    // Seat labels: "BAL-A1" … "BAL-H32", sequential 1..N per row.
    private void seedBalconySeats() {
        for (char r = 'A'; r <= 'C'; r++) seedBalconyRow(String.valueOf(r), 35);
        for (char r = 'D'; r <= 'F'; r++) seedBalconyRow(String.valueOf(r), 30);
        for (char r = 'G'; r <= 'H'; r++) seedBalconyRow(String.valueOf(r), 32);
    }

    private void seedBalconyRow(String row, int totalSeats) {
        List<Seat> seats = new ArrayList<>();
        for (int i = 1; i <= totalSeats; i++) {
            seats.add(new Seat("BAL-" + row + i));
        }
        seatRepository.saveAll(seats);
    }

    // ── Admin Users ──────────────────────────────────────────────────────────
    private void seedAdminUsers() {
        //mr michael hanna
        createSuperAdminIfNotExists("01203813184");
        //bola nasser
        createSuperAdminIfNotExists("01202296329");
        //eriny karam
        createAdminIfNotExists("01211849330");
        //Andrew Kadry
        createAdminIfNotExists("01203996663");
        //peter maher
        createAdminIfNotExists("01070382811");
        //gerges mousa
        createAdminIfNotExists("01068047342");
        //michael saad
        createAdminIfNotExists("01091587701");
    }

    private void createAdminIfNotExists(String phone) {
        if (userRepository.findByPhone(phone).isEmpty()) {
            User admin = new User();
            admin.setPhone(phone);
            admin.setRole("ADMIN");
            admin.setFirstLogin(true);
            userRepository.save(admin);
        }
    }

    private void createSuperAdminIfNotExists(String phone) {
        if (userRepository.findByPhone(phone).isEmpty()) {
            User superAdmin = new User();
            superAdmin.setPhone(phone);
            superAdmin.setRole("SUPER_ADMIN");
            superAdmin.setFirstLogin(true);
            userRepository.save(superAdmin);
        }
    }
}

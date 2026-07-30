package com.stjohn.qalioub.service;

import com.stjohn.qalioub.config.AdminProfile;
import com.stjohn.qalioub.entity.Reservation;
import com.stjohn.qalioub.entity.Seat;
import com.stjohn.qalioub.entity.User;
import com.stjohn.qalioub.repository.ReservationRepository;
import com.stjohn.qalioub.repository.SeatRepository;
import com.stjohn.qalioub.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Service
public class ReservationService {

    public record SeatStatusEntry(Seat seat, Reservation.Status reservationStatus) {}

    private final ReservationRepository reservationRepository;
    private final SeatRepository seatRepository;
    private final UserRepository userRepository;

    @Value("${app.ticket.price}")
    private BigDecimal ticketPrice;

    public ReservationService(ReservationRepository reservationRepository,
                              SeatRepository seatRepository,
                              UserRepository userRepository) {
        this.reservationRepository = reservationRepository;
        this.seatRepository = seatRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Reservation reserveSeats(User user, List<String> seatLabels, String notes) {
        if (seatLabels == null || seatLabels.isEmpty()) {
            throw new IllegalArgumentException("At least one seat must be selected");
        }

        List<String> uniqueLabels = new ArrayList<>(new LinkedHashSet<>(seatLabels));
        List<Seat> seats = seatRepository.findAllByLabelIn(uniqueLabels);
        if (seats.size() != uniqueLabels.size()) {
            throw new IllegalArgumentException("One or more seat labels not found");
        }

        LocalDateTime now = LocalDateTime.now();
        List<Reservation> conflicts = reservationRepository.findActiveReservationsForSeats(seats, now);
        if (!conflicts.isEmpty()) {
            throw new IllegalStateException("One or more seats are already reserved");
        }

        Reservation reservation = new Reservation();
        reservation.setUser(user);
        reservation.setSeats(seats);
        reservation.setStatus(Reservation.Status.PENDING);
        reservation.setCreatedAt(now);
        reservation.setExpiresAt(now.plusHours(4));
        reservation.setNotes(notes);

        reservation = reservationRepository.save(reservation);

        AdminProfile admin = AdminProfile.values()[(int)(reservation.getId() % AdminProfile.values().length)];
        BigDecimal amount = ticketPrice.multiply(BigDecimal.valueOf(seats.size()));
        reservation.setPaymentLink(buildPaymentLink(reservation.getId(), admin, amount, seats.size()));

        return reservationRepository.save(reservation);
    }

    @Transactional(readOnly = true)
    public List<Reservation> getAllReservations() {
        return reservationRepository.findAll();
    }

    @Transactional
    public Reservation confirmReservation(Long id, Long adminId) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + id));

        if (reservation.getStatus() != Reservation.Status.PENDING) {
            throw new IllegalStateException("Only PENDING reservations can be confirmed");
        }
        if (reservation.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Reservation has expired");
        }

        BigDecimal amount = ticketPrice.multiply(BigDecimal.valueOf(reservation.getSeats().size()));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Admin not found: " + adminId));
        admin.setBalance(admin.getBalance().add(amount));
        userRepository.save(admin);

        reservation.setTotalAmount(amount);
        reservation.setStatus(Reservation.Status.CONFIRMED);
        return reservationRepository.save(reservation);
    }

    @Transactional(readOnly = true)
    public List<SeatStatusEntry> getAllSeatsWithStatus() {
        Map<Long, Reservation.Status> statusMap = new LinkedHashMap<>();
        reservationRepository.findActiveReservations(LocalDateTime.now()).forEach(r ->
            r.getSeats().forEach(s ->
                statusMap.merge(s.getId(), r.getStatus(), (existing, incoming) ->
                    existing == Reservation.Status.CONFIRMED ? existing : incoming)
            )
        );

        return seatRepository.findAll().stream()
                .map(seat -> new SeatStatusEntry(seat, statusMap.get(seat.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Reservation> getUserReservations(User user) {
        return reservationRepository.findActiveReservationsByUser(user, LocalDateTime.now());
    }

    private String buildPaymentLink(Long reservationId, AdminProfile admin, BigDecimal amount, int seatCount) {
        String message = String.format(
            "هاي %s\nبكلمك عشان احجز مسرحية الصارخ حجز رقم %d\nهبعتلك دلوقتي %s جنيه على اللينك ده %s\nعشان احجز عدد %d كرسي\nشكرا لتعبك",
            admin.getDisplayName(),
            reservationId,
            amount.stripTrailingZeros().toPlainString(),
            admin.getInstapayLink(),
            seatCount
        );
        String encoded = URLEncoder.encode(message, StandardCharsets.UTF_8);
        return "https://wa.me/" + admin.getWhatsappPhone() + "?text=" + encoded;
    }
}

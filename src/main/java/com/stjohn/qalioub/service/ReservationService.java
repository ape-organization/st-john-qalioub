package com.stjohn.qalioub.service;

import com.stjohn.qalioub.entity.Reservation;
import com.stjohn.qalioub.entity.Seat;
import com.stjohn.qalioub.entity.User;
import com.stjohn.qalioub.repository.ReservationRepository;
import com.stjohn.qalioub.repository.SeatRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public ReservationService(ReservationRepository reservationRepository, SeatRepository seatRepository) {
        this.reservationRepository = reservationRepository;
        this.seatRepository = seatRepository;
    }

    @Transactional
    public Reservation reserveSeats(User user, List<String> seatLabels) {
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
        reservation.setExpiresAt(now.plusHours(2));

        return reservationRepository.save(reservation);
    }

    @Transactional(readOnly = true)
    public List<Reservation> getAllReservations() {
        return reservationRepository.findAll();
    }

    @Transactional
    public Reservation confirmReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + id));

        if (reservation.getStatus() != Reservation.Status.PENDING) {
            throw new IllegalStateException("Only PENDING reservations can be confirmed");
        }
        if (reservation.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Reservation has expired");
        }

        reservation.setStatus(Reservation.Status.CONFIRMED);
        return reservationRepository.save(reservation);
    }

    @Transactional(readOnly = true)
    public List<SeatStatusEntry> getAllSeatsWithStatus() {
        // Build seatId → active reservation status map (CONFIRMED takes priority over PENDING)
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
}

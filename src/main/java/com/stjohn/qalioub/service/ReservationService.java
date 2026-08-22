package com.stjohn.qalioub.service;

import com.stjohn.qalioub.config.AdminProfile;
import com.stjohn.qalioub.entity.CarParking;
import com.stjohn.qalioub.entity.Reservation;import com.stjohn.qalioub.entity.Seat;
import com.stjohn.qalioub.entity.User;
import com.stjohn.qalioub.repository.ReservationRepository;
import com.stjohn.qalioub.repository.SeatRepository;
import com.stjohn.qalioub.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
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
    private final FileStorageService fileStorageService;

    @Value("${app.ticket.price}")
    private BigDecimal ticketPrice;

    @Value("${app.car-parking.fee}")
    private BigDecimal carParkingFee;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    public ReservationService(ReservationRepository reservationRepository,
                              SeatRepository seatRepository,
                              UserRepository userRepository,
                              FileStorageService fileStorageService) {
        this.reservationRepository = reservationRepository;
        this.seatRepository = seatRepository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
    }

    @Transactional
    public Reservation reserveSeats(User user, List<String> seatLabels, String notes,
                                    List<MultipartFile> drivingLicensePhotos) {
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

        List<CarParking> carParkings = buildCarParkings(drivingLicensePhotos);

        Reservation reservation = new Reservation();
        reservation.setUser(user);
        reservation.setSeats(seats);
        reservation.setStatus(Reservation.Status.PENDING);
        reservation.setCreatedAt(now);
        reservation.setExpiresAt(now.plusHours(4));
        reservation.setNotes(notes);

        reservation = reservationRepository.save(reservation);

        for (CarParking cp : carParkings) {
            cp.setReservation(reservation);
        }
        reservation.getCarParkings().addAll(carParkings);
        reservation = reservationRepository.save(reservation);

        AdminProfile admin = AdminProfile.values()[(int)(reservation.getId() % AdminProfile.values().length)];
        BigDecimal amount = calculateAmount(seats.size(), reservation.getCarParkings().size());
        reservation.setAssignedTo(admin.getDisplayName());
        reservation.setPaymentLink(buildPaymentLink(reservation.getId(), admin, amount, user, seats, reservation.getCarParkings()));

        return reservationRepository.save(reservation);
    }

    @Transactional
    public Reservation addCarParkingsToReservation(Long reservationId, User user,
                                                   List<MultipartFile> drivingLicensePhotos) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + reservationId));

        if (!reservation.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Reservation does not belong to this user");
        }

        List<CarParking> newCarParkings = buildCarParkings(drivingLicensePhotos);
        for (CarParking cp : newCarParkings) {
            cp.setReservation(reservation);
            reservation.getCarParkings().add(cp);
        }

        reservation = reservationRepository.save(reservation);

        AdminProfile admin = resolveAdminProfile(reservation.getAssignedTo());
        BigDecimal amount = calculateAmount(reservation.getSeats().size(), reservation.getCarParkings().size());
        reservation.setPaymentLink(buildPaymentLink(reservation.getId(), admin, amount, reservation.getUser(), reservation.getSeats(), reservation.getCarParkings()));

        return reservationRepository.save(reservation);
    }

    @Transactional
    public Reservation consumeSeats(Long reservationId, List<String> seatLabels) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + reservationId));

        if (reservation.getStatus() != Reservation.Status.CONFIRMED) {
            throw new IllegalStateException("Only CONFIRMED reservations can have seats consumed");
        }

        List<String> uniqueLabels = new ArrayList<>(new LinkedHashSet<>(seatLabels));
        List<Seat> seatsToConsume = seatRepository.findAllByLabelIn(uniqueLabels);
        if (seatsToConsume.size() != uniqueLabels.size()) {
            throw new IllegalArgumentException("One or more seat labels not found");
        }

        java.util.Set<Long> reservationSeatIds = reservation.getSeats().stream()
                .map(Seat::getId)
                .collect(java.util.stream.Collectors.toSet());
        seatsToConsume.forEach(seat -> {
            if (!reservationSeatIds.contains(seat.getId())) {
                throw new IllegalArgumentException("Seat " + seat.getLabel() + " does not belong to this reservation");
            }
        });

        java.util.Set<Long> alreadyConsumedIds = reservation.getConsumedSeats().stream()
                .map(Seat::getId)
                .collect(java.util.stream.Collectors.toSet());
        seatsToConsume.stream()
                .filter(s -> !alreadyConsumedIds.contains(s.getId()))
                .forEach(reservation.getConsumedSeats()::add);

        return reservationRepository.save(reservation);
    }

    @Transactional(readOnly = true)
    public List<String> getConfirmedReservationPhones() {
        return reservationRepository.findByStatus(Reservation.Status.CONFIRMED).stream()
                .map(r -> r.getUser().getPhone())
                .distinct()
                .toList();
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

        BigDecimal amount = calculateAmount(reservation.getSeats().size(), reservation.getCarParkings().size());

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Admin not found: " + adminId));
        admin.setBalance(admin.getBalance().add(amount));
        userRepository.save(admin);

        reservation.getCarParkings().forEach(cp -> {
            cp.setStatus(CarParking.Status.CONFIRMED);
            cp.setConfirmedBy(admin.getName());
        });

        reservation.setTotalAmount(amount);
        reservation.setStatus(Reservation.Status.CONFIRMED);
        reservation.setConfirmedBy(admin.getName());
        reservation.setTicketToken(generateTicketToken(reservation.getId(), reservation.getUser().getId()));
        return reservationRepository.save(reservation);
    }

    @Transactional
    public Reservation confirmCarParkings(Long reservationId, Long adminId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + reservationId));

        if (reservation.getStatus() != Reservation.Status.CONFIRMED) {
            throw new IllegalStateException("Reservation must be CONFIRMED before confirming car parkings separately");
        }

        List<CarParking> pending = reservation.getCarParkings().stream()
                .filter(cp -> cp.getStatus() == CarParking.Status.PENDING)
                .toList();

        if (pending.isEmpty()) {
            throw new IllegalStateException("No pending car parkings found on this reservation");
        }

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Admin not found: " + adminId));

        BigDecimal amount = carParkingFee.multiply(BigDecimal.valueOf(pending.size()));
        admin.setBalance(admin.getBalance().add(amount));
        userRepository.save(admin);

        pending.forEach(cp -> {
            cp.setStatus(CarParking.Status.CONFIRMED);
            cp.setConfirmedBy(admin.getName());
        });

        BigDecimal currentTotal = reservation.getTotalAmount() != null ? reservation.getTotalAmount() : BigDecimal.ZERO;
        reservation.setTotalAmount(currentTotal.add(amount));

        return reservationRepository.save(reservation);
    }

    @Transactional
    public void deleteReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + id));

        if (reservation.getStatus() == Reservation.Status.CONFIRMED) {
            String confirmedBy = reservation.getConfirmedBy();
            if (confirmedBy != null) {
                userRepository.findFirstByName(confirmedBy).ifPresent(admin -> {
                    BigDecimal amount = reservation.getTotalAmount() != null
                            ? reservation.getTotalAmount()
                            : calculateAmount(reservation.getSeats().size(), reservation.getCarParkings().size());
                    admin.setBalance(admin.getBalance().subtract(amount));
                    userRepository.save(admin);
                });
            }
        }

        reservationRepository.delete(reservation);
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

    @Transactional(readOnly = true)
    public Reservation getReservationByTicketToken(String token) {
        return reservationRepository.findByTicketToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid ticket token: " + token));
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private List<CarParking> buildCarParkings(List<MultipartFile> drivingLicensePhotos) {
        if (drivingLicensePhotos == null || drivingLicensePhotos.isEmpty()) return new ArrayList<>();

        List<CarParking> result = new ArrayList<>();
        for (MultipartFile licenseFile : drivingLicensePhotos) {
            String licensePath = fileStorageService.saveFile(licenseFile, "driving-licenses");

            CarParking cp = new CarParking();
            cp.setDrivingLicensePhotoPath(licensePath);
            result.add(cp);
        }
        return result;
    }

    private BigDecimal calculateAmount(int seatCount, int carCount) {
        return ticketPrice.multiply(BigDecimal.valueOf(seatCount))
                .add(carParkingFee.multiply(BigDecimal.valueOf(carCount)));
    }

    private AdminProfile resolveAdminProfile(String displayName) {
        if (displayName != null) {
            return Arrays.stream(AdminProfile.values())
                    .filter(a -> a.getDisplayName().equals(displayName))
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    private String generateTicketToken(Long reservationId, Long userId) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String payload = reservationId + ":" + userId;
            byte[] hmacBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hmacBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to generate ticket token", e);
        }
    }

    private String buildPaymentLink(Long reservationId, AdminProfile admin, BigDecimal amount,
                                    User user, List<Seat> seats, List<CarParking> carParkings) {
        String seatLabels = seats.stream().map(Seat::getLabel).collect(java.util.stream.Collectors.joining(", "));

        String carLine = "";
        if (carParkings != null && !carParkings.isEmpty()) {
            BigDecimal carTotal = carParkingFee.multiply(BigDecimal.valueOf(carParkings.size()));
            carLine = String.format(
                "\nوعدد %d عربية في الباركينج بسعر %s جنيه",
                carParkings.size(),
                carTotal.stripTrailingZeros().toPlainString()
            );
        }

        String message = String.format(
            "هاي انا\n%s\nبكلمك عشان احجز مسرحية الصارخ عاوز ابعتلك دلوقتي %s جنيه\nعشان احجز عدد %d كرسي\n(%s)%s\nحجز رقم %d\nشكرا",
            user.getName(),
            amount.stripTrailingZeros().toPlainString(),
            seats.size(),
            seatLabels,
            carLine,
            reservationId
        );
        String encoded = URLEncoder.encode(message, StandardCharsets.UTF_8);
        return "https://wa.me/" + admin.getWhatsappPhone() + "?text=" + encoded;
    }
}


package com.stjohn.qalioub.repository;

import com.stjohn.qalioub.entity.Reservation;
import com.stjohn.qalioub.entity.Seat;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @Override
    @EntityGraph(attributePaths = {"user", "seats"})
    List<Reservation> findAll();

    @Override
    @EntityGraph(attributePaths = {"user", "seats"})
    Optional<Reservation> findById(Long id);

    @Query("""
        SELECT DISTINCT r FROM Reservation r JOIN FETCH r.seats
        WHERE r.status = com.stjohn.qalioub.entity.Reservation.Status.CONFIRMED
           OR (r.status = com.stjohn.qalioub.entity.Reservation.Status.PENDING AND r.expiresAt > :now)
        """)
    List<Reservation> findActiveReservations(@Param("now") LocalDateTime now);

    @Query("""
        SELECT DISTINCT s FROM Reservation r JOIN r.seats s
        WHERE r.status = com.stjohn.qalioub.entity.Reservation.Status.CONFIRMED
           OR (r.status = com.stjohn.qalioub.entity.Reservation.Status.PENDING AND r.expiresAt > :now)
        """)
    List<Seat> findActivelyReservedSeats(@Param("now") LocalDateTime now);

    @Query("""
        SELECT DISTINCT r FROM Reservation r JOIN r.seats s
        WHERE s IN :seats
          AND (r.status = com.stjohn.qalioub.entity.Reservation.Status.CONFIRMED
               OR (r.status = com.stjohn.qalioub.entity.Reservation.Status.PENDING AND r.expiresAt > :now))
        """)
    List<Reservation> findActiveReservationsForSeats(@Param("seats") List<Seat> seats, @Param("now") LocalDateTime now);

    @EntityGraph(attributePaths = {"seats", "user"})
    @Query("""
        SELECT r FROM Reservation r
        WHERE r.user = :user
          AND (r.status = com.stjohn.qalioub.entity.Reservation.Status.CONFIRMED
               OR (r.status = com.stjohn.qalioub.entity.Reservation.Status.PENDING AND r.expiresAt > :now))
        ORDER BY r.createdAt DESC
        """)
    List<Reservation> findActiveReservationsByUser(@Param("user") com.stjohn.qalioub.entity.User user, @Param("now") LocalDateTime now);

    @EntityGraph(attributePaths = {"user", "seats"})
    Optional<Reservation> findByTicketToken(String ticketToken);
}

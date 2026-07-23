package com.stjohn.qalioub.repository;

import com.stjohn.qalioub.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findAllByLabelIn(List<String> labels);
}

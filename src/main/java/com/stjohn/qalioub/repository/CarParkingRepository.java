package com.stjohn.qalioub.repository;

import com.stjohn.qalioub.entity.CarParking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CarParkingRepository extends JpaRepository<CarParking, Long> {
    List<CarParking> findAllByOrderByReservationIdAscIdAsc();
}

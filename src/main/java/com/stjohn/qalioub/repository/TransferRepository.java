package com.stjohn.qalioub.repository;

import com.stjohn.qalioub.entity.Transfer;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransferRepository extends JpaRepository<Transfer, Long> {

    @Override
    @EntityGraph(attributePaths = {"admin"})
    List<Transfer> findAll();

    @Override
    @EntityGraph(attributePaths = {"admin"})
    Optional<Transfer> findById(Long id);
}

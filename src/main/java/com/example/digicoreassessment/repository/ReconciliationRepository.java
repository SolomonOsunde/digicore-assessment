package com.example.digicoreassessment.repository;

import com.example.digicoreassessment.model.Reconciliation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReconciliationRepository extends JpaRepository<Reconciliation, Long> {

    Optional<Reconciliation> findTopByOrderByRanAtDesc();

    Optional<Reconciliation> findByRunId(String runId);
}

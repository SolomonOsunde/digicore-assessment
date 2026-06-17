package com.example.digicoreassessment.repository;

import com.example.digicoreassessment.enums.DiscrepancyCategory;
import com.example.digicoreassessment.model.ReconciliationDiscrepancy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReconciliationDiscrepancyRepository extends JpaRepository<ReconciliationDiscrepancy, Long> {

    List<ReconciliationDiscrepancy> findByRunIdAndCategory(Long runId, DiscrepancyCategory category);

    List<ReconciliationDiscrepancy> findByRunId(Long runId);
}

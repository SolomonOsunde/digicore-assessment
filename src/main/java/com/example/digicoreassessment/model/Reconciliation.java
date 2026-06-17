package com.example.digicoreassessment.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "reconciliation_runs")
@Getter
@Setter
@NoArgsConstructor
public class Reconciliation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String runId;

    @Column(nullable = false)
    private Instant ranAt;

    private int totalInternal;
    private int totalProvider;
    private int matched;
    private int amountMismatches;
    private int statusMismatches;
    private int missingFromProvider;
    private int noProviderReference;
    private int unmatchedProviderRecords;

    @OneToMany(mappedBy = "run", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ReconciliationDiscrepancy> discrepancies = new ArrayList<>();
}

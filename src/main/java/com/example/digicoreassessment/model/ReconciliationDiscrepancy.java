package com.example.digicoreassessment.model;

import com.example.digicoreassessment.enums.DiscrepancyCategory;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "reconciliation_discrepancies")
@Getter
@Setter
@NoArgsConstructor
public class ReconciliationDiscrepancy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "run_id", nullable = false)
    private Reconciliation run;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiscrepancyCategory category;

    private String internalId;
    private String providerReference;

    @Column(precision = 19, scale = 2)
    private BigDecimal internalAmount;

    @Column(precision = 19, scale = 2)
    private BigDecimal providerAmount;

    private String currency;
    private String note;
}

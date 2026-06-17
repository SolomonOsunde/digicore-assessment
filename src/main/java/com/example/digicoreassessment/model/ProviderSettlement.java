package com.example.digicoreassessment.model;

import com.example.digicoreassessment.enums.SettledStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "provider_settlements")
@Getter
@Setter
@NoArgsConstructor
public class ProviderSettlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String providerReference;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SettledStatus settledStatus;

    @Column(nullable = false)
    private Instant settledAt;
}

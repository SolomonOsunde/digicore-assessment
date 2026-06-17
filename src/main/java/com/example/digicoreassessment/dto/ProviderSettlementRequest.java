package com.example.digicoreassessment.dto;

import com.example.digicoreassessment.enums.SettledStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
public class ProviderSettlementRequest {

    @NotBlank
    @Schema(description = "The provider's transaction ID", example = "prov_abc123")
    private String providerReference;

    @NotNull
    @Positive
    @Digits(integer = 17, fraction = 2)
    @Schema(description = "Settled amount, max 2 decimal places", example = "5000.00")
    private BigDecimal amount;

    @NotBlank
    @Schema(description = "ISO currency code", example = "NGN")
    private String currency;

    @NotNull
    @Schema(description = "Settlement status: SETTLED, REVERSED, or PENDING")
    private SettledStatus settledStatus;

    @NotNull
    @Schema(description = "When the settlement was recorded (ISO 8601)", example = "2025-06-01T10:00:00Z")
    private Instant settledAt;
}

package com.example.digicoreassessment.dto;

import com.example.digicoreassessment.enums.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
public class InternalPaymentRequest {

    @NotBlank
    @Schema(description = "Your system's unique payment ID", example = "pay_001")
    private String internalId;

    @Schema(description = "The ID the payment provider assigned (null if payment never reached provider)", example = "prov_abc123")
    private String providerReference;

    @NotNull
    @Positive
    @Digits(integer = 17, fraction = 2)
    @Schema(description = "Payment amount, max 2 decimal places", example = "5000.00")
    private BigDecimal amount;

    @NotBlank
    @Schema(description = "ISO currency code", example = "NGN")
    private String currency;

    @NotNull
    @Schema(description = "Payment status: SUCCESS, FAILED, or PENDING")
    private PaymentStatus status;

    @NotNull
    @Schema(description = "When the payment was initiated (ISO 8601)", example = "2025-06-01T09:00:00Z")
    private Instant initiatedAt;
}

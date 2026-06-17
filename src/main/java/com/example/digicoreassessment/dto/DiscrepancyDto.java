package com.example.digicoreassessment.dto;

import com.example.digicoreassessment.enums.DiscrepancyCategory;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DiscrepancyDto {
    private DiscrepancyCategory category;
    private String internalId;
    private String providerReference;
    private BigDecimal internalAmount;
    private BigDecimal providerAmount;
    private String currency;
    private String note;
}

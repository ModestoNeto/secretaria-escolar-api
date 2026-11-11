package com.modestoneto.secretaria.adapter.in.web;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

record AssessmentRequest(
        @NotBlank @Size(max = 80) String name,
        @NotNull @Min(1) @Max(4) Integer term,
        @NotNull @DecimalMin("0.01") @DecimalMax("999.99") @Digits(integer = 3, fraction = 2) BigDecimal weight,
        @NotNull @DecimalMin("0.01") @DecimalMax("999.99") @Digits(integer = 3, fraction = 2) BigDecimal maxScore) {
}

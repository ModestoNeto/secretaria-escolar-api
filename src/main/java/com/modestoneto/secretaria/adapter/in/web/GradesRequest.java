package com.modestoneto.secretaria.adapter.in.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

// 60 = capacidade máxima de uma turma (CHECK da V3).
record GradesRequest(@NotEmpty @Size(max = 60) List<@Valid Item> grades) {

    record Item(
            @NotNull Long enrollmentId,
            @NotNull @DecimalMin("0") @DecimalMax("999.99") @Digits(integer = 3, fraction = 2) BigDecimal score) {
    }
}

package com.modestoneto.secretaria.adapter.in.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

// 60 = capacidade máxima de uma turma (CHECK da V3).
record AttendanceRequest(@NotEmpty @Size(max = 60) List<@Valid Item> records) {

    record Item(@NotNull Long enrollmentId, @NotNull Boolean present) {
    }
}

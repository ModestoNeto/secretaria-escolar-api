package com.modestoneto.secretaria.adapter.in.web;

import com.modestoneto.secretaria.application.port.in.ClassUseCase;
import com.modestoneto.secretaria.domain.model.Shift;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

record ClassRequest(
        @NotBlank @Size(max = 60) String name,
        @NotNull @Min(1) @Max(12) Integer gradeLevel,
        @NotNull Shift shift,
        @NotNull @Min(2000) @Max(2100) Integer schoolYear,
        @NotNull @Min(1) @Max(60) Integer capacity) {

    ClassUseCase.ClassData toData() {
        return new ClassUseCase.ClassData(name.trim(), gradeLevel, shift, schoolYear, capacity);
    }
}

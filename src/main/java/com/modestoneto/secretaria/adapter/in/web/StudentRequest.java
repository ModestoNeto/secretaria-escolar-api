package com.modestoneto.secretaria.adapter.in.web;

import com.modestoneto.secretaria.application.port.in.StudentUseCase;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

record StudentRequest(
        @NotBlank @Size(max = 160) String fullName,
        @NotNull @Past LocalDate birthDate,
        @NotBlank @Size(max = 160) String guardianName,
        @NotBlank @Pattern(regexp = "\\d{10,11}", message = "deve ter 10 ou 11 dígitos") String guardianPhone,
        @Email @Size(max = 160) String email) {

    StudentUseCase.StudentData toData() {
        String mail = email == null || email.isBlank() ? null : email.trim(); // "" vira null
        return new StudentUseCase.StudentData(fullName.trim(), birthDate, guardianName.trim(), guardianPhone, mail);
    }
}

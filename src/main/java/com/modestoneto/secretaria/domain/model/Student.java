package com.modestoneto.secretaria.domain.model;

import java.time.LocalDate;

public record Student(Long id, String registrationNumber, String fullName, LocalDate birthDate,
                      String guardianName, String guardianPhone, String email, boolean active) {

    public Student deactivated() {
        return new Student(id, registrationNumber, fullName, birthDate, guardianName, guardianPhone, email, false);
    }
}

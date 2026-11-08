package com.modestoneto.secretaria.domain.model;

import java.time.LocalDate;

/** O cpf fica só com os 11 dígitos; é nulo nos alunos cadastrados antes de ele existir. */
public record Student(Long id, String registrationNumber, String fullName, LocalDate birthDate,
                      String guardianName, String guardianPhone, String email, boolean active, String cpf) {

    public Student deactivated() {
        return new Student(id, registrationNumber, fullName, birthDate, guardianName, guardianPhone, email, false, cpf);
    }
}

package com.modestoneto.secretaria.adapter.in.web;

import com.modestoneto.secretaria.domain.model.Cpf;
import com.modestoneto.secretaria.domain.model.Student;

import java.time.LocalDate;

// cpf sai com máscara (000.000.000-00); no banco ele fica só com os dígitos
record StudentResponse(Long id, String registrationNumber, String fullName, LocalDate birthDate,
                       String guardianName, String guardianPhone, String email, boolean active, String cpf) {

    static StudentResponse from(Student s) {
        return new StudentResponse(s.id(), s.registrationNumber(), s.fullName(), s.birthDate(),
                s.guardianName(), s.guardianPhone(), s.email(), s.active(), Cpf.mask(s.cpf()));
    }
}

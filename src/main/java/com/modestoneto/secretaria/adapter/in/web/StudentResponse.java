package com.modestoneto.secretaria.adapter.in.web;

import com.modestoneto.secretaria.domain.model.Student;

import java.time.LocalDate;

record StudentResponse(Long id, String registrationNumber, String fullName, LocalDate birthDate,
                       String guardianName, String guardianPhone, String email, boolean active) {

    static StudentResponse from(Student s) {
        return new StudentResponse(s.id(), s.registrationNumber(), s.fullName(), s.birthDate(),
                s.guardianName(), s.guardianPhone(), s.email(), s.active());
    }
}

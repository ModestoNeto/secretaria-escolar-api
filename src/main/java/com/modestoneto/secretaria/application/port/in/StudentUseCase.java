package com.modestoneto.secretaria.application.port.in;

import com.modestoneto.secretaria.domain.model.Student;

import java.time.LocalDate;

public interface StudentUseCase {

    record StudentData(String fullName, LocalDate birthDate, String guardianName,
                       String guardianPhone, String email) {
    }

    Student create(StudentData data);

    Student update(Long id, StudentData data);

    Student findById(Long id);

    /** Desativa sem apagar; repetir a chamada não muda nada. */
    void deactivate(Long id);
}

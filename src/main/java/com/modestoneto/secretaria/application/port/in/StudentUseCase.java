package com.modestoneto.secretaria.application.port.in;

import com.modestoneto.secretaria.application.common.PageRequest;
import com.modestoneto.secretaria.application.common.PageResult;
import com.modestoneto.secretaria.domain.model.Student;

import java.time.LocalDate;
import java.util.Set;

public interface StudentUseCase {

    /** Campos permitidos na ordenação; qualquer outro vira 400. */
    Set<String> SORT_FIELDS = Set.of("fullName", "registrationNumber", "birthDate");

    record StudentData(String fullName, LocalDate birthDate, String guardianName,
                       String guardianPhone, String email) {
    }

    Student create(StudentData data);

    Student update(Long id, StudentData data);

    Student findById(Long id);

    /** Desativa sem apagar; repetir a chamada não muda nada. */
    void deactivate(Long id);

    /** q: trecho do nome, sem distinguir acento nem caixa (vazio devolve todos). */
    PageResult<Student> search(String q, boolean includeInactive, PageRequest pageRequest);
}

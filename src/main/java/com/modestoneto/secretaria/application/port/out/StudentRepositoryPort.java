package com.modestoneto.secretaria.application.port.out;

import com.modestoneto.secretaria.domain.model.Student;

import java.util.Optional;

public interface StudentRepositoryPort {

    /** Id nulo cria o aluno e gera a matrícula; id preenchido atualiza. */
    Student save(Student student);

    Optional<Student> findById(Long id);
}
